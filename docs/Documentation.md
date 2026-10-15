---
layout: page
title: Documentation guide
---

## Website and guides

GitHub Pages publishes the team's `master` branch from `docs/` using Jekyll. Navigation and site metadata are configured in `docs/_config.yml`; shared styles are in `docs/_sass`.

Keep the main user instructions in `UserGuide.md` and implementation/design/manual-test material in `DeveloperGuide.md`. Update the README and `index.md` when the advertised product changes. `images/Ui.png` must be an actual screenshot of the current app using realistic synthetic data.

After changes, inspect the rendered pages as well as the Markdown source. Check internal links, tables, image sizes and navigation. [Jekyll setup](https://se-education.org/guides/tutorials/jekyll.html) explains local preview; GitHub Pages build status is available in the repository's Actions/Pages settings.

## Diagrams and attribution

Keep PlantUML sources under `diagrams/` and their rendered images under `images/`. Regenerate the image when changing its source and verify that the diagram matches the implemented APIs. See the [PlantUML tutorial](https://se-education.org/guides/tutorials/plantUml.html).

Retain AB3 attribution and document AI assistance accurately in the Developer Guide acknowledgements. Do not assign another contributor's work or AI usage to yourself.

## Style and export

Use clear task-based instructions, copyable examples and descriptive links. The [Google documentation style guide](https://developers.google.com/style) is useful editorial guidance. See [saving documentation as PDF](https://se-education.org/guides/tutorials/savingPdf.html) if an export is needed; verify the exported layout separately.
