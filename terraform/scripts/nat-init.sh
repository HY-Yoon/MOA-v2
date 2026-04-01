#!/bin/bash
set -euo pipefail

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

# 4. Nginx 설정 파일 생성 (80포트 요청을 App 서버 사설 IP:8080포트로 전달)
cat > /etc/nginx/conf.d/app-proxy.conf << 'NGINX_CONF'
server {
    listen 80;
    server_name moa.hee-factory.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_CONF

# 5. Nginx 설정 테스트 후 재시작
nginx -t && systemctl restart nginx
