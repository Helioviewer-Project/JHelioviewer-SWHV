# SAMP automation

These scripts control a running JHelioviewer through SAMP. They are automation
tools, not regression tests.

- `jhv_samp_daemon.py` connects to a running SAMP hub, finds JHelioviewer, and
  accepts queued commands through a local Unix socket. It requires Astropy.
- `jhv_samp_client.py` sends commands to that socket. It uses only the Python
  standard library.

Start JHelioviewer and connect it to a SAMP hub before starting the daemon:

```sh
~/jhv-validator/bin/python extra/samp/jhv_samp_daemon.py
```

Use a Python environment containing Astropy. The daemon uses
`/tmp/jhv-samp.sock` by default. Both scripts accept `--socket PATH` to select
another socket. Run one daemon per socket. These scripts require Unix sockets.

From another terminal, send a command:

```sh
python3 extra/samp/jhv_samp_client.py raw \
  '{"mtype":"jhv.playback.pause"}'
```

The `raw` argument accepts a JSON command or an array of commands. Each command
has an `mtype` and an optional `params` object. The daemon executes them in order.
For image loading, state loading, and recording, it waits for JHV's completion
notification before continuing. Other commands return a `sent` status, which
only confirms dispatch. The client prints the results as JSON.

## Movie example

```sh
python3 extra/samp/jhv_samp_client.py movie /path/to/fits/files --size H1080
```

This clears the current image layers, loads the supplied file or directory,
records one playback loop at 25 frames per second, and pauses playback.
`H1080` is the default recording size. Directory loading is recursive.

The image adjustments are explicitly set in `movie_commands()` in the client,
including opacity, sharpening, FITS clipping, and gamma. Edit that example or
send your own command array to choose other settings.

The daemon waits without a timeout for completion notifications. If JHV stops
responding, the request and subsequent queued commands remain blocked. Stop the
daemon with Ctrl-C and restart it after restarting or reconnecting JHV.
