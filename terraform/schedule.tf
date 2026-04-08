# ==============================================================================
# 평일 운영 스케줄 (비용 절감)
# - NAT는 앱보다 5분 먼저 기동, 5분 늦게 종료
# - EventBridge Scheduler로 NAT EC2 start/stop 호출
# ==============================================================================

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

resource "aws_iam_role" "scheduler_ec2_role" {
  count = var.enable_business_hours_schedule ? 1 : 0
  name  = "moa-v2-scheduler-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "scheduler.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "scheduler_ec2_policy" {
  count = var.enable_business_hours_schedule ? 1 : 0
  name  = "moa-v2-scheduler-ec2-policy"
  role  = aws_iam_role.scheduler_ec2_role[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = [
        "ec2:StartInstances",
        "ec2:StopInstances"
      ]
      Effect = "Allow"
      Resource = format(
        "arn:aws:ec2:%s:%s:instance/%s",
        data.aws_region.current.name,
        data.aws_caller_identity.current.account_id,
        aws_instance.nat_instance.id
      )
    }]
  })
}

resource "aws_scheduler_schedule" "nat_weekday_start" {
  count = var.enable_business_hours_schedule ? 1 : 0

  name                         = "moa-v2-nat-weekday-start"
  schedule_expression          = var.nat_start_cron
  schedule_expression_timezone = var.business_hours_timezone
  state                        = "ENABLED"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:ec2:startInstances"
    role_arn = aws_iam_role.scheduler_ec2_role[0].arn
    input    = jsonencode({ InstanceIds = [aws_instance.nat_instance.id] })
  }
}

resource "aws_scheduler_schedule" "nat_weekday_stop" {
  count = var.enable_business_hours_schedule ? 1 : 0

  name                         = "moa-v2-nat-weekday-stop"
  schedule_expression          = var.nat_stop_cron
  schedule_expression_timezone = var.business_hours_timezone
  state                        = "ENABLED"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:ec2:stopInstances"
    role_arn = aws_iam_role.scheduler_ec2_role[0].arn
    input    = jsonencode({ InstanceIds = [aws_instance.nat_instance.id] })
  }
}
