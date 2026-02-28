import { BE_URL } from '@/constants/common/url';
import { NextRequest, NextResponse } from 'next/server';

type Props = { params: Promise<{ path: string[] }> };

async function proxyRequest(request: NextRequest, { params }: Props) {
  try {
    const { path } = await params;
    const pathString = path.join('/');
    const url = new URL(request.url);

    const contentType = request.headers.get('content-type') ?? 'application/json';
    const headers: Record<string, string> = {
      'Content-Type': contentType,
    };
    const authHeader = request.headers.get('authorization');
    const cookie = request.headers.get('cookie');
    if (authHeader) headers['Authorization'] = authHeader;
    if (cookie) headers['Cookie'] = cookie;

    const fetchOptions: RequestInit = {
      method: request.method,
      headers,
    };

    let bodySizeBytes = 0;
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)) {
      const body = await request.text();
      if (body) {
        // 이미지 등록 파일 용량 체크
        bodySizeBytes = Buffer.byteLength(body, 'utf8');
        if (bodySizeBytes > 0) {
          const mb = (bodySizeBytes / 1024 / 1024).toFixed(2);
          console.log(
            `[Proxy] ${request.method} /api/${pathString} size: ${bodySizeBytes.toLocaleString()} bytes (${mb} MB) / type: ${contentType.slice(0, 30)} ...`,
          );
        }
        fetchOptions.body = body;
      }
    }

    const backendUrl = `${BE_URL}/api/${pathString}${url.search}`;
    const response = await fetch(backendUrl, fetchOptions);
    const data = await response.text();

    return new NextResponse(data, {
      status: response.status,
      headers: {
        'Content-Type': response.headers.get('Content-Type') || 'application/json',
      },
    });
  } catch (error) {
    console.error('Proxy Error:', error);
    return NextResponse.json({ error: 'Failed to proxy request' }, { status: 500 });
  }
}

export async function GET(request: NextRequest, props: Props) {
  return proxyRequest(request, props);
}

export async function POST(request: NextRequest, props: Props) {
  return proxyRequest(request, props);
}

export async function PUT(request: NextRequest, props: Props) {
  return proxyRequest(request, props);
}

export async function DELETE(request: NextRequest, props: Props) {
  return proxyRequest(request, props);
}

export async function PATCH(request: NextRequest, props: Props) {
  return proxyRequest(request, props);
}
