#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${1:-ap-northeast-2}"
ASG_NAME="${2:-moa-v2-app-asg}"
NAT_TAG_NAME="${3:-moa-v2-nat-instance}"

echo "[1/3] App ASG 정지: ${ASG_NAME}"
aws autoscaling update-auto-scaling-group \
  --region "$AWS_REGION" \
  --auto-scaling-group-name "$ASG_NAME" \
  --min-size 0 \
  --max-size 1 \
  --desired-capacity 0

echo "[2/3] NAT 인스턴스 ID 조회 (${NAT_TAG_NAME})"
NAT_INSTANCE_ID=$(aws ec2 describe-instances \
  --region "$AWS_REGION" \
  --filters "Name=tag:Name,Values=${NAT_TAG_NAME}" "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query "Reservations[].Instances[].InstanceId" \
  --output text | awk '{print $1}')

if [ -z "${NAT_INSTANCE_ID:-}" ] || [ "$NAT_INSTANCE_ID" = "None" ]; then
  echo "NAT 인스턴스를 찾지 못했습니다. (tag:Name=${NAT_TAG_NAME})"
  exit 1
fi

echo "[3/3] NAT 인스턴스 정지: ${NAT_INSTANCE_ID}"
aws ec2 stop-instances --region "$AWS_REGION" --instance-ids "$NAT_INSTANCE_ID" >/dev/null
aws ec2 wait instance-stopped --region "$AWS_REGION" --instance-ids "$NAT_INSTANCE_ID"

echo "완료: APP + NAT 정지"
