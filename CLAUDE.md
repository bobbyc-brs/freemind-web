# freemind-web — Project Instructions

## Orientation

Read `VISION.md` first, then `PLAN.md`. Work happens on the **`web`** branch.

- `master` mirrors upstream FreeMind (SourceForge, remote `upstream`) —
  never commit to it; it moves only by pulling upstream.
- `web` = master + the web interface + everything in PLAN.md.
- The deep analysis docs (HLD with file:line defect assessment, DECISIONS DD
  log) live in the companion project
  `~/Projects/freemind-code/` — read them there; do not copy them here.

## Writing style

Documentation follows `~/Projects/standards/BOBBYC_STYLE.md`: Canadian
English, one idea per paragraph, verb-led bullets, quantified claims, every
code claim cited as `file:line`, decisions Bobby owns marked ⚖ and left for
his approval.

## Layout and build

- `freemind/` — upstream desktop app (Java 8 / Swing / Ant): `cd freemind &&
  ant dist`. Touch only what a milestone requires; upstream files change
  with restraint.
- `src/` + `pom.xml` — web module (Java 8 / Jetty 9.4 / Maven):
  `mvn clean package`, run with `mvn exec:java` → `http://localhost:8080`.
  Port 8080 may be taken on this machine (HL7 tester) — the M1 `WebServer`
  class should make the port configurable.
- `target/` is gitignored; never commit build output.

## Rules

- Tests land with the milestone, not after it (see each milestone's exit
  test in PLAN.md).
- Never reimplement `.mm` serialization — reuse the desktop serializer
  (that's M2's whole point).
- Commits: imperative subject, body says why; end with
  `Co-Authored-By:` trailer per your harness conventions.
- Push to `origin` (github.com/bobbyc-brs/freemind-web) `web` branch;
  never push to `upstream`.
