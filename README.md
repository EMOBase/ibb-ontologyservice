# Ontology Service for the iBeetle-Base project

## Development

Prerequisites:
- [quarkus-cli](https://quarkus.io/guides/cli-tooling) is installed.

To run the application in dev mode, run:
```bash
quarkus dev
```

These commands might be useful after a long time not working on this project:

- `quarkus update`: Update all quarkus dependencies to latest versions
- `./mvnw -N wrapper:wrapper -Dmaven=3.8.7`: Update maven wrapper to new version, in this case `3.8.7`

## Deployment

Images are published to GHCR by `.github/workflows/publish.yml`:

```
ghcr.io/emobase/ibb-ontologyservice:<tag>
```

To build locally:

```bash
./mvnw package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t emobase/ibb-ontologyservice:local .
```

The container needs an Elasticsearch instance at `es:9200`. Set
`ONTOLOGYSERVICE_ELASTICSEARCH_INDEX_PREFIX` to namespace the indices and
aliases it creates; the default is `ontologyservice`.
