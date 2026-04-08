$ErrorActionPreference = "Stop"

Set-Location -Path (Join-Path $PSScriptRoot "..")

Write-Host "==> Terraform init"
terraform init

Write-Host "==> Terraform destroy"
terraform destroy -auto-approve

Write-Host ""
Write-Host "완료: 인프라가 삭제되었습니다."
