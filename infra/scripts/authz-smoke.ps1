# Authorization smoke test against the local gateway.
# Prereqs: `make up`, and two users with password Admin123!:
#   admin@ppms.local (SYSTEM_ADMIN) and farm.manager@ppms.local (FARM_MANAGER on one farm).
$ErrorActionPreference = 'Continue'
$gw = 'http://localhost:8080'

function Login($email, $pw) {
  $body = @{ email = $email; password = $pw; deviceId = 'smoke' } | ConvertTo-Json -Compress
  foreach ($i in 1..3) {
    $r = $body | curl.exe -s -X POST "$gw/api/identity/auth/login" -H 'Content-Type: application/json' -d '@-' | ConvertFrom-Json
    if ($r.accessToken) { return $r }
    Start-Sleep 2
  }
  throw "login failed for $email"
}

function Call($token, $method, $path, $body) {
  $args = @('-s', '-o', 'NUL', '-w', '%{http_code}', '-X', $method, "$gw$path", '-H', "Authorization: Bearer $token", '-H', 'Content-Type: application/json')
  if ($body) { $args += @('-d', '@-'); return ($body | & curl.exe @args) }
  return (& curl.exe @args)
}

$admin = Login 'admin@ppms.local' 'Admin123!'
$farm  = Login 'farm.manager@ppms.local' 'Admin123!'
"admin roles: $($admin.user.roles -join ',')   farm roles: $($farm.user.roles -join ',')"
""
"{0,-45} {1,8} {2,8}" -f 'endpoint', 'ADMIN', 'FARM_MGR'
foreach ($c in @(
  @('GET',  '/api/farm/farms'),
  @('POST', '/api/farm/farms', '{}'),
  @('GET',  '/api/hatchery/sites'),
  @('POST', '/api/hatchery/sites', '{}'),
  @('GET',  '/api/parent-flock/sites'),
  @('GET',  '/api/slaughterhouse/sites'),
  @('GET',  '/api/reporting/sites'),
  @('GET',  '/api/admin/employees'),
  @('GET',  '/api/admin/breeds'),
  @('POST', '/api/admin/breeds', '{}'),
  @('GET',  '/api/admin/settings/definitions')
)) {
  $m, $p, $b = $c
  "{0,-45} {1,8} {2,8}" -f "$m $p", (Call $admin.accessToken $m $p $b), (Call $farm.accessToken $m $p $b)
}
