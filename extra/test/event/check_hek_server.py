#!/usr/bin/env python3
"""Read-only HEK regression probe using captured records from every configured supplier."""

import datetime
import json
from urllib.parse import urlencode
from urllib.request import urlopen


BASE = {
    "cmd": "search", "type": "column", "cosec": 2,
    "event_coordsys": "helioprojective", "x1": -3600, "x2": 3600, "y1": -3600, "y2": 3600,
    "result_limit": 500, "page": 1,
}


def fetch(params):
    url = "https://www.lmsal.com/hek/her?" + urlencode(BASE | params)
    with urlopen(url, timeout=45) as response:
        data = json.load(response)
    assert isinstance(data["overmax"], bool), "Missing or invalid pagination metadata"
    return data


def ids(data):
    return [event["kb_archivid"] for event in data["result"]]


def paginated(params):
    found = []
    for page in range(1, 101):
        data = fetch(params | {"page": page})
        found.extend(ids(data))
        if not data["overmax"]:
            assert len(found) == len(set(found)), "Duplicate events across pages"
            return set(found)
    raise AssertionError("Unexpected pagination loop")


# Captured record identities and intervals keep this probe independent of the large replay fixture.
CASES = [
    ('FL', 'SWPC', '2024-05-10T00:10:00', '2024-05-10T00:22:00', 'ivo://helio-informatics.org/FL_SWPC_20240510_065326_20240510001000'),
    ('FL', 'Flare Detective - Trigger Module', '2024-05-10T00:03:08', '2024-05-10T00:06:08', 'ivo://helio-informatics.org/FL_FlareDetective-TriggerModule_20240510_071417_2024-05-10T00:03:08.066_1'),
    ('CE', 'CACTus (Computer Aided CME Tracking)', '2024-05-10T07:12:07', '2024-05-10T08:48:07', 'ivo://helio-informatics.org/CE_CACTus(ComputerAidedCMETracking)_20241017_095056_2410.52'),
    ('AR', 'NOAA SWPC Observer', '2024-05-10T00:00:00', '2024-05-10T23:59:59', 'ivo://helio-informatics.org/AR_NOAASWPCObserver_20240510_024826_NOAA13663_20240510'),
    ('AR', 'SPoCA', '2024-05-09T20:42:46', '2024-05-10T00:42:46', 'ivo://helio-informatics.org/AR_SPoCA_20240516_143144_20240510T004246_0'),
    ('CH', 'SPoCA', '2024-05-09T20:42:41', '2024-05-10T00:42:41', 'ivo://helio-informatics.org/CH_SPoCA_20240516_143156_20240510T004241_0'),
    ('SS', 'EGSO_SFC', '2026-09-03T13:42:34', '2026-09-03T19:42:34', 'ivo://helio-informatics.org/SS_EGSO_SFC_20260903_155630_2026.09.03T13.42.34.800_57'),
    ('CD', 'halocme', '2010-12-29T02:59:58', '2010-12-29T12:59:58', 'ivo://helio-informatics.org/CD211_halocme_20110113_013229'),
    ('CD', 'Coronal Dimming Module', '2013-03-01T03:05:06', '2013-03-01T09:10:06', 'ivo://helio-informatics.org/CD_CoronalDimmingModule_20130522_191159_DE-20130522191123692-CGBJIE'),
    ('CW', 'halocme', '2010-05-12T13:30:05', '2010-05-12T15:30:05', 'ivo://helio-informatics.org/CW171_halocme_20120322_212746'),
    ('FI', 'AAFDCC', '2010-03-30T10:39:29', '2010-03-30T10:39:29', 'ivo://helio-informatics.org/FI_AAFDCC_20100330_152237_20100330_103929_01'),
    ('FE', 'halocme', '2010-06-11T22:00:18', '2010-06-12T11:57:00', 'ivo://helio-informatics.org/FE171_NariakiNitta_20100712_201445'),
    ('EF', 'Emerging flux region module', '2011-03-01T01:12:45', '2011-03-01T02:12:45', 'ivo://helio-informatics.org/EF_Emergingfluxregionmodule_20110331_234836_2011-03-01T01:12:45_0'),
    ('ER', 'EruptionPatrol', '2024-05-10T00:23:05', '2024-05-10T01:03:05', 'ivo://helio-informatics.org/ER_EruptionPatrol_20240510_0043_-954.000_-111.600'),
]

for event_type, supplier, event_start, event_end, archive_id in CASES:
    start = datetime.datetime.fromisoformat(event_start)
    end = datetime.datetime.fromisoformat(event_end)
    midpoint = start + (end - start) / 2
    params = {
        "event_type": event_type,
        "event_starttime": (midpoint - datetime.timedelta(seconds=1)).isoformat(timespec="seconds"),
        "event_endtime": (midpoint + datetime.timedelta(seconds=1)).isoformat(timespec="seconds"),
    }
    reference = fetch(params)
    assert not reference["overmax"], "Reference window must fit in one response"
    expected = set(ids(reference))
    assert archive_id in expected, f"Missing overlapping record for {supplier}"
    assert paginated(params | {"result_limit": 3}) == expected, f"Pagination lost events for {supplier}"
    selected = [e for e in reference["result"] if e["frm_name"] == supplier]
    assert selected, f"No records after local supplier filtering for {supplier}"
    print(json.dumps({"type": event_type, "supplier": supplier, "received": len(expected), "selected": len(selected)}), flush=True)

# The supplier predicate introduced in eb8df6079 returned zero here despite real CACTus records.
cactus = {
    "event_type": "CE", "event_starttime": "2024-05-10T00:00:00", "event_endtime": "2024-05-12T00:00:00",
}
reference = fetch(cactus)
assert not reference["overmax"]
expected = {e["kb_archivid"] for e in reference["result"] if e["frm_name"] == "CACTus (Computer Aided CME Tracking)"}
assert expected, "CACTus reference must not be empty"
assert paginated(cactus | {"result_limit": 3}) == set(ids(reference)), "CACTus pagination lost events"
filtered = fetch(cactus | {"param0": "FRM_Name", "op0": "=", "value0": "CACTus (Computer Aided CME Tracking)"})
print(json.dumps({"cactus_without_predicate": len(expected), "cactus_with_predicate": len(ids(filtered)), "suppliers_checked": len(CASES)}))
