# PayEvidence — run on your own machine

Demo tenant: **NordicPay**. Thymeleaf demo UI + REST API (Days 11–15).

## 1. Prerequisites

Install:

| Tool | Version | Check |
|------|---------|--------|
| **JDK** | 21+ | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **jq** (optional) | any | prettier JSON in curl |

### macOS (Homebrew)
```bash
brew install openjdk@21 maven jq
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

### Windows (winget / Chocolatey)
- Install “Microsoft Build of OpenJDK 21” or Temurin 21
- Install Maven, or use the Maven wrapper once we add it
- Git Bash or PowerShell for curl

### Ubuntu / Debian
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk-headless maven jq
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

You do **not** need Docker or Postgres for the default demo (uses local H2 file DB).

## 2. Get the project

Use the folder you received (`payevidence/`), or unpack:

```bash
tar -xzf payevidence-local-setup.tar.gz
cd payevidence/app
```

Folder layout that must stay intact:

```
payevidence/
  day1/          # catalogue + sample artifacts
  day2-3/        # scoring docs + change_record sample
  app/           # Spring Boot project  ← run from here
```

## 3. Start the app (API + demo UI)

```bash
cd payevidence/app
export JAVA_HOME=...   # from step 1
export PATH="$JAVA_HOME/bin:$PATH"

mvn -q test            # optional sanity check — should PASS
mvn -q spring-boot:run
```

Wait until you see `Started PayEvidenceApplication`. It listens on **http://localhost:8080**.

On first start it auto-seeds **NordicPay** (default profile → about **16 pass / 4 gap**).


## 3b. Open the demo UI

Default token: **`nordic-demo`** (override with `PAYEVIDENCE_DEMO_TOKEN`).

| URL | What |
|-----|------|
| http://localhost:8080/demo?token=nordic-demo | NordicPay dashboard (sets cookie) |
| http://localhost:8080/demo/gate | Token form |
| http://localhost:8080/ | Redirects to `/demo` |

**Auth story:** UI pages need the token (query or cookie). **`/api/**` stays open** for local curl. On a public VPS put basic-auth in front — see `app/DEPLOY.md`.

Happy path: chips → filter Gaps → control detail → Reset / change-record profile → Generate evidence pack → View HTML / Download PDF.

5-minute rehearsal: `DEMO-SCRIPT-5MIN.md`.

## 4. Test it yourself (copy-paste — API)

Open a second terminal:

```bash
BASE=http://localhost:8080

# Seed (safe to repeat)
curl -s -X POST "$BASE/api/demo/seed" | jq .

# Find NordicPay
ORG=$(curl -s "$BASE/api/orgs/by-name/NordicPay" | jq -r .id)
echo "ORG=$ORG"

# Show only failing controls
curl -s "$BASE/api/orgs/$ORG/controls?status=gap" | jq '{pass,gap,stale,gaps:[.controls[].controlId]}'

# Build an evidence pack (HTML + PDF)
PACK=$(curl -s -X POST "$BASE/api/orgs/$ORG/evidence-packs" | jq -r .id)
echo "PACK=$PACK"

# Download files to your Downloads (adjust path)
curl -s -o ~/Downloads/nordicpay-pack.html "$BASE/api/orgs/$ORG/evidence-packs/$PACK/download?format=html"
curl -s -o ~/Downloads/nordicpay-pack.pdf  "$BASE/api/orgs/$ORG/evidence-packs/$PACK/download?format=pdf"
open ~/Downloads/nordicpay-pack.html   # macOS; on Linux: xdg-open ...
```

### Second story — change gaps fixed, vuln still red

```bash
curl -s -X POST "$BASE/api/demo/seed?demoProfile=withChangeRecords" | jq '{pass,gap,demoProfile}'
ORG=$(curl -s "$BASE/api/orgs/by-name/NordicPay" | jq -r .id)
curl -s "$BASE/api/orgs/$ORG/controls?status=gap" | jq '{pass,gap,gaps:[.controls[].controlId]}'
# Expect: pass=18 gap=2 (only vuln controls)
```

## 5. What “good” looks like

| Step | Expected |
|------|----------|
| `mvn test` | BUILD SUCCESS |
| Default seed | pass **16**, gap **4** |
| Gap list | change 6.5/6.3 + vuln 6.4/11.3 |
| HTML/PDF download | opens; says **NordicPay** |
| withChangeRecords | pass **18**, gap **2** (vuln only) |

## 6. Common problems

| Problem | Fix |
|---------|-----|
| `java: command not found` | Install JDK 21 and set `JAVA_HOME` |
| Port 8080 in use | Stop the other app, or run with `-Dserver.port=8081` |
| Seed finds no samples | Run from `payevidence/app` so `../day1` resolves |
| Windows path issues | Run from Git Bash; keep `day1` next to `app` |

## 7. Optional Postgres later

```bash
cd payevidence/app
docker compose up -d
mvn -q spring-boot:run -Dspring-boot.run.profiles=postgres
```

Default H2 is enough for local learning and demos.
