#!/usr/bin/env bash
set -euo pipefail

sudo mv /tmp/moa-backend.service /etc/systemd/system/moa-backend.service
sudo chmod 644 /etc/systemd/system/moa-backend.service

sudo systemctl daemon-reload
sudo systemctl enable moa-backend.service
