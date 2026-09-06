#!/usr/bin/env python3
"""Read-only HEK contract probe. Requires network access, uses a fixed historical window."""

import json
from urllib.parse import urlencode
from urllib.request import urlopen


BASE = {
    "cmd": "search", "type": "column", "cosec": 2, "event_type": "fl",
    "event_coordsys": "helioprojective", "x1": -3600, "x2": 3600, "y1": -3600, "y2": 3600,
    "event_starttime": "2024-05-10T00:00:00", "event_endtime": "2024-05-10T02:00:00",
    "result_limit": 500, "page": 1,
}


def fetch(**params):
    url = "https://www.lmsal.com/hek/her?" + urlencode(BASE | params)
    with urlopen(url, timeout=45) as response:
        data = json.load(response)
    assert isinstance(data["overmax"], bool), "Missing or invalid pagination metadata"
    return data


def ids(data):
    return [event["kb_archivid"] for event in data["result"]]


all_events = fetch()
assert not all_events["overmax"]
filtered = fetch(param0="FRM_Name", op0="=", value0="SWPC")
assert not filtered["overmax"]
assert all(e["frm_name"] == "SWPC" for e in filtered["result"])
assert set(ids(filtered)) == {e["kb_archivid"] for e in all_events["result"] if e["frm_name"] == "SWPC"}
legacy_filter = fetch(param0="frm_name", op0="==", value0="SWPC")

first = fetch(page=1, result_limit=3)
zero = fetch(page=0, result_limit=3)
page_ids = ids(first)
page_count = 1
page = first
while page["overmax"]:
    page_count += 1
    assert page_count <= 20, "Unexpected pagination loop"
    page = fetch(page=page_count, result_limit=3)
    page_ids.extend(ids(page))
assert len(page_ids) == len(set(page_ids)), "Duplicate events across pages"
assert set(page_ids) == set(ids(all_events)), "Pagination lost events"

overlap = fetch(event_starttime="2024-05-10T00:04:00", event_endtime="2024-05-10T00:05:00")
assert not overlap["overmax"]
expected = {e["kb_archivid"] for e in all_events["result"]
            if e["event_starttime"] <= "2024-05-10T00:05:00" and e["event_endtime"] >= "2024-05-10T00:04:00"}
assert set(ids(overlap)) == expected and expected, "Overlap contract changed"
print(json.dumps({
    "all_events": len(ids(all_events)), "swpc_equal": len(ids(filtered)),
    "swpc_double_equal": len(ids(legacy_filter)), "page_zero_aliases_one": ids(zero) == ids(first),
    "pages": page_count, "unique_paginated_events": len(page_ids), "overlapping_events": len(expected),
}, indent=2))
