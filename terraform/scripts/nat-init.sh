#!/bin/bash
set -euo pipefail

timedatectl set-timezone Asia/Seoul

# NAT (iptables masquerade)
dnf install -y iptables-services
systemctl enable iptables

sysctl -w net.ipv4.ip_forward=1
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf

ETH=$(ip route show default | awk '/default/ {print $5}')
iptables -t nat -A POSTROUTING -o "$ETH" -j MASQUERADE
iptables-save > /etc/sysconfig/iptables

# Swap 2GB (fallocate는 dd보다 즉시 할당되어 부팅 시간 단축)
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo "/swapfile swap swap defaults 0 0" >> /etc/fstab
fi

# Nginx
dnf install -y nginx
setsebool -P httpd_can_network_connect 1 || true
systemctl enable --now nginx

# 앱 서버 IP 조회 → Nginx 프록시 설정 스크립트 (초기 + cron 공용)
cat > /opt/refresh-nginx-upstream.sh << 'REFRESH_EOF'
#!/bin/bash
APP_IP=$(aws ec2 describe-instances \
  --region ap-northeast-2 \
  --filters "Name=tag:Name,Values=moa-v2-app-instance" \
            "Name=instance-state-name,Values=running" \
  --query "Reservations[0].Instances[0].PrivateIpAddress" \
  --output text 2>/dev/null) || exit 0

[ -z "$APP_IP" ] || [ "$APP_IP" = "None" ] && exit 0

CURRENT=$(grep -oP 'proxy_pass http://\K[0-9.]+' /etc/nginx/conf.d/app-proxy.conf 2>/dev/null) || CURRENT=""

if [ "$APP_IP" != "$CURRENT" ]; then
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
  nginx -t && systemctl reload nginx
fi
REFRESH_EOF
chmod +x /opt/refresh-nginx-upstream.sh

# 초기 부팅 시: 앱 서버가 올라올 때까지 대기 후 Nginx 설정
echo "Waiting for backend app server to become available..."
while true; do
  /opt/refresh-nginx-upstream.sh && \
    [ -f /etc/nginx/conf.d/app-proxy.conf ] && \
    grep -q 'proxy_pass' /etc/nginx/conf.d/app-proxy.conf 2>/dev/null && break
  echo "Backend not found yet. Retrying in 5 seconds..."
  sleep 5
done
echo "Nginx upstream configured."

# cron — 매분 앱 서버 IP 변경 감지 및 자동 갱신 (ASG 교체 대응)
dnf install -y cronie
systemctl enable --now crond
echo "* * * * * root /opt/refresh-nginx-upstream.sh" > /etc/cron.d/refresh-nginx
chmod 644 /etc/cron.d/refresh-nginx
