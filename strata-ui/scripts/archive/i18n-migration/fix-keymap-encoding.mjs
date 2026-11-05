import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const file = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  "../src/shared/i18n/keyMap.ts",
);
const fixes = {
  "auth.register.creating": "Creating\u2026",
  "common.loading": "Loading\u2026",
  "common.saving": "Saving\u2026",
  "auth.common.sending": "Sending\u2026",
  "auth.signingIn": "Signing in\u2026",
  "auth.reset.updating": "Updating\u2026",
  "system.files.uploading": "Uploading\u2026",
  "auth.login.skipTenant": "Skip \u2014 sign in without tenant selection",
  "common.emDash": "\u2014",
};

const lines = fs.readFileSync(file, "utf8").split("\n");
for (let i = 0; i < lines.length; i++) {
  for (const [key, label] of Object.entries(fixes)) {
    if (lines[i].includes(`"${key}"`)) {
      lines[i] = `  "${label}": "${key}",`;
    }
  }
}
fs.writeFileSync(file, lines.join("\n"));
console.log("keyMap lines fixed");
