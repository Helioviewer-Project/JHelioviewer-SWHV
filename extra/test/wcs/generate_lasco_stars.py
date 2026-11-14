"""Write star pairs of Helioviewer LASCO JP2 frames for WcsInterpreterTest.

usage: generate_lasco_stars.py stars.bin out.json frame.pgm:frame.xml [...]

The PGM is the JP2 decoded at full resolution (opj_decompress), the XML its original
header. Image stars are matched to the catalog through the header CRPIX and CDELT and a
fitted similarity transform, independent of the geometry under test. Catalog positions
are seen from SOHO (swhv.oma.be/position) with proper motion and aberration.
"""

import json
import struct
import sys
import urllib.request

import numpy as np


def read_pgm(path):
    data = open(path, "rb").read()
    fields, pos = [], 0
    while len(fields) < 4:
        while data[pos:pos + 1].isspace():
            pos += 1
        if data[pos:pos + 1] == b"#":
            pos = data.index(b"\n", pos) + 1
            continue
        end = pos
        while not data[end:end + 1].isspace():
            end += 1
        fields.append(data[pos:end])
        pos = end
    w, h = int(fields[1]), int(fields[2])
    return np.frombuffer(data[pos + 1:], dtype=np.uint8, count=w * h).reshape(h, w).astype(float)


def header(text, key):
    start = text.index("<" + key + ">") + len(key) + 2
    return text[start:text.index("<", start)].strip()


def box(img, r):
    c = np.cumsum(np.cumsum(np.pad(img, ((r + 1, r), (r + 1, r)), mode="edge"), 0), 1)
    n = 2 * r + 1
    return (c[n:, n:] - c[:-n, n:] - c[n:, :-n] + c[:-n, :-n]) / n / n


# Peaks above six noise sigmas in a high-pass image, centroids in a 5x5 window.
def detect(img):
    valid = img > 2
    hp = img - box(img, 7)
    noise = 1.4826 * np.median(np.abs(hp[valid] - np.median(hp[valid])))
    h, w = img.shape
    yy, xx = np.mgrid[-2:3, -2:3]
    peaks = []
    for y, x in np.argwhere((hp > 6 * noise) & valid):
        if y < 5 or x < 5 or y > h - 6 or x > w - 6:
            continue
        win = hp[y - 2:y + 3, x - 2:x + 3]
        if hp[y, x] < win.max() or not valid[y - 4:y + 5, x - 4:x + 5].all():
            continue
        wv = np.clip(win, 0, None)
        peaks.append((x + (wv * xx).sum() / wv.sum(), y + (wv * yy).sum() / wv.sum()))
    return np.array(peaks)


# Helioprojective longitude and latitude [arcsec] of catalog stars seen from SOHO.
def catalog(stars, utc, magnitude):
    data = open(stars, "rb").read()
    epoch = struct.unpack(">d", data[12:20])[0]
    n = struct.unpack(">i", data[20:24])[0]
    rec = np.dtype([("ra", ">f8"), ("dec", ">f8"), ("pmra", ">f4"), ("pmdec", ">f4"), ("plx", ">f4"),
                    ("rv", ">f4"), ("mag", ">f4"), ("hip", ">i4")])
    cat = np.frombuffer(data, dtype=rec, count=n, offset=24)
    t0 = np.datetime64(utc)
    url = ("https://swhv.oma.be/position?ref=J2000&observer=SOHO&target=SUN&abcorr=LT%%2BS&utc=%s&utc_end=%s&deltat=60"
           % (str(t0), str(t0 + np.timedelta64(60, "s"))))
    res = json.load(urllib.request.urlopen(url))["result"]
    p0, p1 = np.array(list(res[0].values())[0]), np.array(list(res[1].values())[0])
    vel = -(p1 - p0) / 60.0
    s = p0 / np.linalg.norm(p0)
    ra0, dec0 = np.radians(286.13), np.radians(63.87)
    pole = np.array([np.cos(dec0) * np.cos(ra0), np.cos(dec0) * np.sin(ra0), np.sin(dec0)])
    north = pole - (pole @ s) * s
    north /= np.linalg.norm(north)
    west = np.cross(s, north)
    years = 2000 + (t0 - np.datetime64("2000-01-01T12:00:00")) / np.timedelta64(1, "D") / 365.25 - epoch
    dec = np.radians(cat["dec"]) + np.radians(cat["pmdec"] / 3.6e6) * years
    ra = np.radians(cat["ra"]) + np.radians(cat["pmra"] / 3.6e6) * years / np.cos(np.radians(cat["dec"]))
    u = np.stack([np.cos(dec) * np.cos(ra), np.cos(dec) * np.sin(ra), np.sin(dec)], 1)
    beta = vel / 299792.458
    u = u + beta - (u @ beta)[:, None] * u
    u /= np.linalg.norm(u, axis=1)[:, None]
    u = u[(u @ s > np.cos(np.radians(10))) & (cat["mag"] < magnitude)]
    lon = np.degrees(np.arctan2(u @ west, u @ s)) * 3600
    lat = np.degrees(np.arctan2(u @ north, np.hypot(u @ west, u @ s))) * 3600
    return np.stack([lon, lat], 1)


def match(img_xy, cat_xy, radius):
    pairs = []
    for i, (x, y) in enumerate(img_xy):
        d = np.hypot(cat_xy[:, 0] - x, cat_xy[:, 1] - y)
        j = np.argmin(d)
        if d[j] < radius:
            pairs.append((i, j))
    return np.array(pairs, dtype=int).reshape(-1, 2)


def frame(stars, pgm, xml):
    text = open(xml).read()
    text = text[text.index("<"):]  # drop a leading XML box label
    detector = header(text, "DETECTOR")
    utc = header(text, "DATE_OBS").replace("/", "-") + "T" + header(text, "TIME_OBS")[:8]
    cdelt = float(header(text, "CDELT1"))
    crpix = np.array([float(header(text, "CRPIX1")), float(header(text, "CRPIX2"))])
    img = read_pgm(pgm)
    h = img.shape[0]
    peaks = detect(img)
    cat_xy = catalog(stars, utc, 9 if detector == "C2" else 7.5)
    # Header reading: FITS x = column + 1, FITS y = height - row, linear about CRPIX
    img_xy = (np.stack([peaks[:, 0] + 1, h - peaks[:, 1]], 1) - crpix) * cdelt
    model = img_xy
    for _ in range(4):
        pairs = match(model, cat_xy, 4 * cdelt)
        if len(pairs) < 3:
            print("%s %s: %d matches, skipped" % (detector, utc, len(pairs)))
            return None
        a = np.zeros((2 * len(pairs), 4))
        p = img_xy[pairs[:, 0]]
        a[0::2] = np.stack([p[:, 0], -p[:, 1], np.ones(len(p)), np.zeros(len(p))], 1)
        a[1::2] = np.stack([p[:, 1], p[:, 0], np.zeros(len(p)), np.ones(len(p))], 1)
        prm, *_ = np.linalg.lstsq(a, cat_xy[pairs[:, 1]].reshape(-1), rcond=None)
        model = np.stack([prm[0] * img_xy[:, 0] - prm[1] * img_xy[:, 1] + prm[2],
                          prm[1] * img_xy[:, 0] + prm[0] * img_xy[:, 1] + prm[3]], 1)
    pairs = match(model, cat_xy, 3 * cdelt)
    # JHV buffer coordinates: pixel centers at column + 0.5 and row + 0.5, rows from the top
    rows = [[round(peaks[i, 0] + .5, 3), round(peaks[i, 1] + .5, 3),
             round(cat_xy[j, 0], 3), round(cat_xy[j, 1], 3)] for i, j in pairs]
    print("%s %s: %d stars" % (detector, utc, len(rows)))
    return {"header": text, "stars": rows}


def main():
    stars, out = sys.argv[1], sys.argv[2]
    frames = [f for f in (frame(stars, *arg.split(":")) for arg in sys.argv[3:]) if f is not None]
    with open(out, "w") as f:
        json.dump(frames, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    main()
