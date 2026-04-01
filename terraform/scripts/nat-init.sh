#!/bin/bash
set -uo pipefail

# 0. 타임존을 한국 시간(KST)으로 변경
timedatectl set-timezone Asia/Seoul

# 1. 기존 NAT 기능(iptables) 유지
dnf install -y iptables-services
systemctl enable iptables

sysctl -w net.ipv4.ip_forward=1
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf

ETH=$(ip route show default | awk '/default/ {print $5}')

iptables -t nat -A POSTROUTING -o "$ETH" -j MASQUERADE
iptables-save > /etc/sysconfig/iptables

# 2. EBS 활용 Swap 4GB 생성 및 활성화 (RAM 부족 방지용)
dd if=/dev/zero of=/swapfile bs=1M count=4096
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo "/swapfile swap swap defaults 0 0" >> /etc/fstab

# 3. Nginx 설치 및 실행
dnf install -y nginx
systemctl enable --now nginx

# 앱 서버 IP를 동적으로 조회하기 위한 스크립트 (켜질 때까지 무한 대기)
echo "Waiting for backend app server to become available..."
while true; do
  APP_IP=$(aws ec2 describe-instances \
    --region ap-northeast-2 \
    --filters "Name=tag:Name,Values=moa-v2-app-instance" \
              "Name=instance-state-name,Values=running" \
    --query "Reservations[0].Instances[0].PrivateIpAddress" \
    --output text) || APP_IP=""
  
  # APP_IP가 None이 아니고 비어있지 않은 경우 루프 탈출
  if [ "${APP_IP:-}" != "None" ] && [ -n "${APP_IP:-}" ]; then
    echo "Found Backend IP: $APP_IP"
    break
  fi
  echo "Backend not found yet. Retrying in 5 seconds..."
  sleep 5
done

# 찾아낸 앱 서버 IP($APP_IP)를 Nginx proxy_pass에 동적으로 주입
cat > /etc/nginx/conf.d/app-proxy.conf << NGINX_CONF
server {
    listen 80;
    server_name moa.hee-factory.com;

    location / {
        proxy_pass http://${APP_IP}:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINX_CONF

# 5. Nginx 설정 테스트 후 재시작
nginx -t && systemctl restart nginx
