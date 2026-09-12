# Additional data servers

Add servers to `~/JHelioviewer-SWHV/Settings/sources.json` and restart JHelioviewer.
The file can contain both image API servers and HAPI servers, as illustrated in
[`extra/sources.json`](../extra/sources.json).

## Image API servers

Add entries under `org.helioviewer.jhv.source.image`:

```json
{
  "org.helioviewer.jhv.source.image": [
    {
      "name": "GSFC Beta",
      "label": "Goddard Space Flight Center Beta Server",
      "api": "https://api.beta.helioviewer.org/v2/"
    }
  ]
}
```

All three fields are required:

- `name` identifies the server in JHV.
- `label` supplies its descriptive display label.
- `api` is the Helioviewer API base URL, including the trailing `/`. JHV appends
  the `getDataSources/`, `getJP2Image/`, and `getJPX/` endpoints to this URL.

The optional `availability` field supplies the URL prefix for the **Available data** button in the image selection dialog. JHV appends `ID=<sourceId>` directly,
so include the query separator, for example
`"availability": "https://swhv.oma.be/availability/?"`. Omit this field if the
server has no compatible availability page.

## HAPI servers

Add entries under `org.helioviewer.jhv.source.hapi`:

```json
{
  "org.helioviewer.jhv.source.hapi": [
    {
      "name": "ROB Test",
      "api": "http://swhv-test:4000/hapi/"
    }
  ]
}
```

`name` identifies the server group in the timeline selection dialog. `api` is
its HAPI base URL; a missing trailing slash is added automatically. Each server
supplies its own catalog, dataset metadata, and any JHV predefined groups.

## Configuration behavior

Both arrays can coexist in the same JSON object.

User servers appear in file order. For repeated names, the last entry supplies
the configuration. Built-in server names take precedence over user entries: `ROB`, `IAS`, and
`GSFC` for image servers, and `ROB` for HAPI servers. Names are independent
between the two server types. Invalid entries are logged and skipped.

**Reload Datasets Listings** refreshes the remote catalogs. It does not reread
`sources.json`; configuration changes require a restart.
