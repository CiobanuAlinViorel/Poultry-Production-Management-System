# Exercise the access-management API (roles / permissions / assignments / users) through the gateway as the admin.
# Idempotent: safe to run repeatedly against a dev stack.
$ErrorActionPreference = 'Stop'
$gw = 'http://localhost:8080'
function Api($method, $path, $body, $token) {
  $args = @('-s', '-w', "`n%{http_code}", '-X', $method, "$gw$path", '-H', 'Content-Type: application/json')
  if ($token) { $args += @('-H', "Authorization: Bearer $token") }
  if ($body) { $args += @('-d', '@-'); $out = ($body | & curl.exe @args) } else { $out = (& curl.exe @args) }
  $lines = $out -split "`n"; $code = $lines[-1]; $json = ($lines[0..($lines.Length - 2)] -join "`n")
  return @{ code = $code; body = $json }
}
function Json($r) { try { return $r.body | ConvertFrom-Json } catch { return $null } }

$login = Api POST '/api/identity/auth/login' '{"email":"admin@ppms.local","password":"Admin123!","deviceId":"smoke"}'
$tok = (Json $login).accessToken
if (-not $tok) { throw "login failed: $($login.body)" }

$r = Api GET '/api/identity/users?page=0&size=50&sort=createdAt,desc' $null $tok
$users = Json $r
"GET users                          -> $($r.code)  total=$($users.totalElements)"
$ids = ($users.content | ForEach-Object { $_.id }) -join ','
$r = Api GET "/api/admin/access/assignments/by-users?userIds=$ids" $null $tok
"GET assignments/by-users           -> $($r.code)  users-with-roles=$(((Json $r).PSObject.Properties | Measure-Object).Count)"
$r = Api GET '/api/admin/access/roles' $null $tok;        "GET roles                          -> $($r.code)  count=$((Json $r).Count)"
$r = Api GET '/api/admin/access/permissions' $null $tok;  "GET permissions                    -> $($r.code)  count=$((Json $r).Count)"
$r = Api GET '/api/admin/access/sites?scopeType=FARM' $null $tok; "GET sites?scopeType=FARM           -> $($r.code)  count=$((Json $r).Count)"

$r = Api POST '/api/admin/access/permissions' '{"key":"farm:report:read","description":"Smoke test permission"}' $tok
"POST permission                    -> $($r.code)  (201 first run, 400 duplicate afterwards)"
$r = Api POST '/api/admin/access/roles' '{"code":"smoke_reporter","nameRo":"Raportor smoke","module":"FARM","allowedScopeTypes":["FARM","GLOBAL"],"permissions":["farm:report:read"]}' $tok
"POST role                          -> $($r.code)  (201 first run, 400 duplicate afterwards)"
$role = (Json (Api GET '/api/admin/access/roles' $null $tok)) | Where-Object { $_.code -eq 'SMOKE_REPORTER' }
if (-not $role.active) { $null = Api PUT "/api/admin/access/roles/$($role.id)" '{"code":"smoke_reporter","nameRo":"Raportor smoke","active":true,"allowedScopeTypes":["FARM","GLOBAL"],"permissions":["farm:report:read"]}' $tok }
"     role SMOKE_REPORTER            perms=$($role.permissions -join ',') scopes=$($role.allowedScopeTypes -join ',')"

$farmMgr = $users.content | Where-Object { $_.email -eq 'farm.manager@ppms.local' } | Select-Object -First 1
$r = Api POST '/api/admin/access/assignments' "{`"userId`":`"$($farmMgr.id)`",`"roleCode`":`"SMOKE_REPORTER`",`"scopeType`":`"GLOBAL`",`"reason`":`"smoke`"}" $tok
$asg = Json $r
"POST assignment                    -> $($r.code)  $($asg.roleCode)@$($asg.scopeType) active=$($asg.active)"
$r = Api POST '/api/admin/access/assignments' "{`"userId`":`"$($farmMgr.id)`",`"roleCode`":`"SMOKE_REPORTER`",`"scopeType`":`"GLOBAL`"}" $tok
"POST duplicate assignment          -> $($r.code)  $((Json $r).code)"
$r = Api POST '/api/admin/access/assignments' "{`"userId`":`"$($farmMgr.id)`",`"roleCode`":`"SMOKE_REPORTER`",`"scopeType`":`"HATCHERY`",`"scopeId`":`"$([guid]::NewGuid())`"}" $tok
"POST disallowed scope              -> $($r.code)  $((Json $r).code)"
$r = Api GET "/api/admin/access/assignments?userId=$($farmMgr.id)" $null $tok
"GET assignments?userId             -> $($r.code)  $(((Json $r) | ForEach-Object { $_.roleCode }) -join ',')"
$r = Api DELETE "/api/admin/access/assignments/$($asg.id)" $null $tok; "DELETE assignment                  -> $($r.code)"
$r = Api PUT "/api/admin/access/roles/$($role.id)" '{"code":"smoke_reporter","nameRo":"Raportor smoke","active":false,"allowedScopeTypes":["GLOBAL"]}' $tok
"PUT role (deactivate)              -> $($r.code)  active=$((Json $r).active)"

$email = "smoke.$([guid]::NewGuid().ToString().Substring(0,8))@ppms.local"
$r = Api POST '/api/identity/users' "{`"email`":`"$email`",`"displayName`":`"Smoke User`",`"initialPassword`":`"Smoke123!x`",`"sendActivationEmail`":false}" $tok
"POST user (initial password)       -> $($r.code)  status=$((Json $r).status)"
$r = Api POST '/api/identity/users' "{`"email`":`"$email`",`"sendActivationEmail`":false}" $tok
"POST duplicate user                -> $($r.code)  $((Json $r).code)"
$r = Api POST '/api/identity/auth/login' "{`"email`":`"$email`",`"password`":`"Smoke123!x`",`"deviceId`":`"smoke`"}"
"login as new user                  -> $($r.code)"
$r = Api GET '/api/identity/users?q=smoke' $null $tok; "GET users?q=smoke                  -> $($r.code)  total=$((Json $r).totalElements)"
$r = Api GET '/api/identity/users?status=LOCKED' $null $tok; "GET users?status=LOCKED            -> $($r.code)  total=$((Json $r).totalElements)"
