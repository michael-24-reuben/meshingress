cd J:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress\toolspace\cobalt\upstream\cobalt

corepack enable
corepack prepare pnpm@9.6.0 --activate

pnpm install --frozen-lockfile
Set-Content -Path api\.env -Value "API_URL=http://localhost:9000/"

cd api
pnpm start 