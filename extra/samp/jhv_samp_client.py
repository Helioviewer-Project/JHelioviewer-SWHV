#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import socket
from pathlib import Path
from urllib.parse import urlparse

SOCKET = "/tmp/jhv-samp.sock"


def send(commands: list[dict], socket_path: str = SOCKET) -> list[dict]:
    with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as client:
        client.connect(socket_path)
        client.sendall(json.dumps(commands).encode("utf-8") + b"\n")
        response = json.loads(client.makefile("rb").readline())
    if not isinstance(response, dict):
        raise RuntimeError(f"Unexpected daemon response: {response!r}")
    if not response.get("ok"):
        raise RuntimeError(response.get("error", "JHV SAMP request failed"))
    return response["results"]


def command(mtype: str, params: dict | None = None) -> dict:
    return {"mtype": mtype, "params": params or {}}


def as_url(value: str) -> str:
    parsed = urlparse(value)
    if parsed.scheme:
        return value
    return Path(value).expanduser().resolve().as_uri()


def empty_state() -> str:
    return json.dumps(
        {
            "org.helioviewer.jhv.state": {
                "layers": [],
                "imageLayers": [],
                "timelines": [],
            },
        }
    )


def movie_commands(path: str, size: str) -> list[dict]:
    return [
        # send empty or preferred state to clear previously loaded image layers
        command(
            "jhv.load.state",
            {
                "value": empty_state(),
            },
        ),
        # those are global, no need to always set them
        command(
            "jhv.view.fits.set",
            {
                "value": json.dumps(
                    {
                        "clippingMode": "ZScale",
                        "zContrast": 40,
                        "scalingMode": "Gamma",
                        "gamma": 0.4545454545,
                    }
                ),
            },
        ),
        command(
            "jhv.load.image",
            {
                "url": as_url(path),
                "imageParams": json.dumps(
                    {
                        "opacity": 0.75,
                        "sharpen": 0.2,
                        "enhanced": 1.0,
                    }
                ),
            },
        ),
        command(
            "jhv.record.start",
            {
                "mode": "LOOP",
                "size": size,
                "advanceMode": "Loop",
                "speed": 25,
                "speedUnit": "FRAMES_PER_SECOND",
            },
        ),
        # need to pause after recording, otherwise the movie keeps playing
        command("jhv.playback.pause"),
    ]


def commands_from_json(value: str) -> list[dict]:
    request = json.loads(value)
    if isinstance(request, list):
        return request
    return [request]


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Send queued SAMP commands to JHelioviewer"
    )
    parser.add_argument("--socket", default=SOCKET)
    subparsers = parser.add_subparsers(dest="mode", required=True)

    raw = subparsers.add_parser("raw")
    raw.add_argument("json")

    movie = subparsers.add_parser("movie")
    movie.add_argument("path")
    movie.add_argument("--size", default="H1080")

    args = parser.parse_args()
    if args.mode == "raw":
        print(json.dumps(send(commands_from_json(args.json), args.socket), indent=2))
    else:
        print(
            json.dumps(
                send(movie_commands(args.path, args.size), args.socket), indent=2
            )
        )


if __name__ == "__main__":
    main()
