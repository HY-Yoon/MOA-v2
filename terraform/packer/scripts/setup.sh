#!/usr/bin/env bash
set -euo pipefail

sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto-headless

sudo useradd --system --home /opt/moa --shell /usr/sbin/nologin moa || true
sudo mkdir -p /opt/moa/backend
sudo chown -R moa:moa /opt/moa

sudo mv /tmp/app.jar /opt/moa/backend/app.jar
sudo chown moa:moa /opt/moa/backend/app.jar
