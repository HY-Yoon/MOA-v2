# ==============================================================================
# S3 Bucket Configuration for Image Uploads
# ==============================================================================

resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

resource "aws_s3_bucket" "moa_cdn_bucket" {
  bucket = "moa-v2-cdn-bucket-${random_string.bucket_suffix.result}"
  
  tags = {
    Name = "moa-v2-cdn-bucket"
  }
}

resource "aws_s3_bucket_public_access_block" "moa_cdn_bucket_public_block" {
  bucket = aws_s3_bucket.moa_cdn_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_cors_configuration" "moa_cdn_bucket_cors" {
  bucket = aws_s3_bucket.moa_cdn_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = ["*"]
    max_age_seconds = 3000
  }
}

# ==============================================================================
# CloudFront Configuration (CDN)
# ==============================================================================
resource "aws_cloudfront_origin_access_control" "moa_oac" {
  name                              = "moa-v2-s3-oac"
  description                       = "OAC for Moa S3 Bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "moa_cdn" {
  origin {
    domain_name              = aws_s3_bucket.moa_cdn_bucket.bucket_regional_domain_name
    origin_id                = "Moa-S3-Origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.moa_oac.id
  }

  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = ""

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD", "OPTIONS"]
    target_origin_id = "Moa-S3-Origin"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
      headers = ["Origin", "Access-Control-Request-Headers", "Access-Control-Request-Method"]
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 86400    # 24 hours
    max_ttl                = 31536000 # 365 days
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "moa-v2-cloudfront"
  }
}

# ==============================================================================
# S3 Bucket Policy to allow CloudFront Access
# ==============================================================================
data "aws_iam_policy_document" "s3_policy" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.moa_cdn_bucket.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.moa_cdn.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "moa_cdn_bucket_policy" {
  bucket = aws_s3_bucket.moa_cdn_bucket.id
  policy = data.aws_iam_policy_document.s3_policy.json
}
