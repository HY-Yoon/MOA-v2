const fs = require('fs');
const path = require('path');

const sourceDir = path.join(__dirname, '../../@shared');
const targetDir = path.join(__dirname, '../shared');

// 타겟 디렉토리가 있으면 삭제
if (fs.existsSync(targetDir)) {
  fs.rmSync(targetDir, { recursive: true, force: true });
}

// 타겟 디렉토리 생성
fs.mkdirSync(targetDir, { recursive: true });

// 파일 복사 함수
function copyRecursiveSync(src, dest) {
  const exists = fs.existsSync(src);
  const stats = exists && fs.statSync(src);
  const isDirectory = exists && stats.isDirectory();

  if (isDirectory) {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest);
    }
    fs.readdirSync(src).forEach((childItemName) => {
      copyRecursiveSync(
        path.join(src, childItemName),
        path.join(dest, childItemName)
      );
    });
  } else {
    fs.copyFileSync(src, dest);
  }
}

// @shared 내용 복사
console.log('📦 Copying @shared to @frontend/shared...');
copyRecursiveSync(sourceDir, targetDir);
console.log('✅ @shared copied successfully!');
