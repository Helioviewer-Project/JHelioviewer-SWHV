#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import signal
import socketserver
import uuid
from pathlib import Path
from threading import Event

from astropy.samp import SAMPIntegratedClient


SOCKET = "/tmp/jhv-samp.sock"

COMPLETIONS = {
    "jhv.load.image": "jhv.load.image.completed",
    "jhv.load.state": "jhv.load.state.completed",
    "jhv.record.start": "jhv.record.start.completed",
}


class Pending:
    def __init__(self, mtype: str) -> None:
        self.mtype = mtype
        self.event = Event()
        self.params = None


class JHVSampQueue:
    def __init__(self) -> None:
        self.client = SAMPIntegratedClient(name="JHV SAMP queue")
        self.client_id = None
        self.jhv_id = None
        self.pending = {}

    def __enter__(self):
        self.client.connect()
        self.client_id = self.client.get_public_id()
        for completion_mtype in COMPLETIONS.values():
            self.client.bind_receive_notification(completion_mtype, self.completion)
        self.jhv_id = self.find_jhv()
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> None:
        self.client.disconnect()

    def find_jhv(self):
        for client_id in self.client.get_registered_clients():
            if self.client.get_metadata(client_id).get("samp.name") == "JHelioviewer":
                return client_id
        raise RuntimeError("JHelioviewer SAMP client not found")

    def execute(self, commands: list[dict]) -> list[dict]:
        results = []
        for command in commands:
            results.append(self.execute_one(command))
        return results

    def execute_one(self, command: dict) -> dict:
        mtype = command.get("mtype")
        params = command.get("params", {})
        if not isinstance(mtype, str) or not mtype:
            raise ValueError("mtype must be a non-empty string")
        if not isinstance(params, dict):
            raise ValueError("params must be an object")

        if mtype in COMPLETIONS:
            return self.request(mtype, params)
        self.notify(mtype, params)
        return {"mtype": mtype, "status": "sent"}

    def notify(self, mtype: str, params: dict) -> None:
        self.client.notify(self.jhv_id, {"samp.mtype": mtype, "samp.params": to_samp(params)})

    def request(self, mtype: str, params: dict) -> dict:
        request_id = str(uuid.uuid4())
        params = dict(params)
        params["requestId"] = request_id
        pending = Pending(mtype)
        self.pending[request_id] = pending
        try:
            self.notify(mtype, params)
            pending.event.wait()
            if pending.params["status"] != "success":
                raise RuntimeError(pending.params.get("message", f"{mtype} failed"))
            return pending.params
        finally:
            self.pending.pop(request_id, None)

    def completion(self, private_key, sender_id, mtype, params, extra) -> None:
        if params.get("clientId") != self.client_id:
            return
        pending = self.pending.get(params.get("requestId"))
        if pending is None:
            return
        if params.get("mtype") != pending.mtype:
            pending.params = {
                "status": "failure",
                "message": f"Unexpected completion for {params.get('mtype')}, expected {pending.mtype}",
            }
        else:
            pending.params = dict(params)
        pending.event.set()


class Server(socketserver.UnixStreamServer):
    def __init__(self, path: str, queue: JHVSampQueue) -> None:
        self.queue = queue
        super().__init__(path, Handler)


class Handler(socketserver.StreamRequestHandler):
    def handle(self) -> None:
        try:
            request = json.loads(self.rfile.readline())
            commands = request if isinstance(request, list) else [request]
            if not isinstance(commands, list) or not all(isinstance(command, dict) for command in commands):
                raise ValueError("request must be a command or a list of commands")
            response = {"ok": True, "results": self.server.queue.execute(commands)}
        except Exception as e:
            response = {"ok": False, "error": str(e)}
        self.wfile.write(json.dumps(response).encode("utf-8") + b"\n")


def to_samp(value):
    if isinstance(value, dict):
        return {str(k): to_samp(v) for k, v in value.items()}
    if isinstance(value, list):
        return [to_samp(v) for v in value]
    if isinstance(value, bool):
        return "true" if value else "false"
    if value is None:
        return ""
    return str(value)


def remove_socket(path: str) -> None:
    socket_path = Path(path)
    try:
        if socket_path.is_dir():
            raise IsADirectoryError(path)
        socket_path.unlink()
    except FileNotFoundError:
        pass


def stop(signum, frame) -> None:
    raise SystemExit(0)


def main() -> None:
    parser = argparse.ArgumentParser(description="Queue SAMP commands to JHelioviewer")
    parser.add_argument("--socket", default=SOCKET)
    args = parser.parse_args()

    remove_socket(args.socket)
    with JHVSampQueue() as queue:
        with Server(args.socket, queue) as server:
            os.chmod(args.socket, 0o600)
            signal.signal(signal.SIGINT, stop)
            signal.signal(signal.SIGTERM, stop)
            print(f"Listening on {args.socket}", flush=True)
            try:
                server.serve_forever()
            finally:
                remove_socket(args.socket)


if __name__ == "__main__":
    main()
