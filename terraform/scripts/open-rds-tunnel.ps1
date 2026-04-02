$ErrorActionPreference = "Stop"

param(
  [string]$NatNameTag = "moa-v2-nat-instance",
  [string]$DbIdentifier = "moa-v2-postgres",
  [int]$RemotePort = 5432,
  [int]$LocalPort = 5432
)

function Require-Command {
  param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "필수 명령어를 찾을 수 없습니다: $Name"
  }
}

Require-Command -Name "aws"

Write-Host "==> NAT 인스턴스 조회 중..."
$natInstanceId = aws ec2 describe-instances `
  --filters "Name=tag:Name,Values=$NatNameTag" "Name=instance-state-name,Values=running" `
  --query "Reservations[].Instances[].InstanceId" `
  --output text

if (-not $natInstanceId -or $natInstanceId -eq "None") {
  throw "실행 중인 NAT 인스턴스를 찾지 못했습니다. tag:Name=$NatNameTag"
}

Write-Host "NAT Instance ID: $natInstanceId"

Write-Host "==> RDS 엔드포인트 조회 중..."
$rdsEndpoint = aws rds describe-db-instances `
  --db-instance-identifier $DbIdentifier `
  --query "DBInstances[0].Endpoint.Address" `
  --output text

if (-not $rdsEndpoint -or $rdsEndpoint -eq "None") {
  throw "RDS 엔드포인트를 찾지 못했습니다. db identifier=$DbIdentifier"
}

Write-Host "RDS Endpoint: $rdsEndpoint"
Write-Host "로컬 포트 $LocalPort -> 원격 $rdsEndpoint`:$RemotePort 포워딩을 시작합니다."
Write-Host "종료하려면 Ctrl+C를 누르세요."
Write-Host ""

$paramsObj = @{
  host            = @($rdsEndpoint)
  portNumber      = @("$RemotePort")
  localPortNumber = @("$LocalPort")
} | ConvertTo-Json -Compress

aws ssm start-session `
  --target $natInstanceId `
  --document-name "AWS-StartPortForwardingSessionToRemoteHost" `
  --parameters $paramsObj
