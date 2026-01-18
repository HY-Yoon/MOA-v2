/**
 * SeatGroup을 API 요청용 Seat 배열로 변환
 * ❗ 이 함수는 저장 시에만 1회 호출됨
 * ❗ 에디터 내부에서는 절대 사용하지 않음
 * ✅ anchor 기준 회전 적용 (origin 기준)
 */
export function flattenSeatGroupsToSeats(
  seatGroups: Seat.SeatGroup[],
  seatRadius: number
): Seat.SeatRequest[] {
  const seats: Seat.SeatRequest[] = [];

  seatGroups.forEach((group) => {
    const {
      sectionId,
      origin,
      rows,
      columns,
      seatGap,
      rowGap,
      rotation,
      flipHorizontal,
      flipVertical,
      curved,
      curveAmount,
      rowLabelConfig,
      seatLabelConfig,
    } = group;

    // ✅ origin 기준 회전 (API 저장 시에는 origin 기준)
    const rotatePoint = (x: number, y: number): { x: number; y: number } => {
      if (rotation === 0) return { x, y };
      
      const rad = (rotation * Math.PI) / 180;
      const dx = x - origin.x;
      const dy = y - origin.y;
      
      return {
        x: origin.x + dx * Math.cos(rad) - dy * Math.sin(rad),
        y: origin.y + dx * Math.sin(rad) + dy * Math.cos(rad),
      };
    };

    // 행×열 순회하여 개별 좌석 생성
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < columns; c++) {
        // 좌석 위치 계산
        let x = origin.x + c * (seatRadius * 2 + seatGap);
        let y = origin.y + r * (seatRadius * 2 + rowGap);

        // 곡선 효과
        if (curved && curveAmount) {
          const progress = c / (columns - 1);
          const totalWidth = (seatRadius * 2 + seatGap) * columns - seatGap;
          const curveOffset = Math.sin(progress * Math.PI) * totalWidth * curveAmount;
          y += curveOffset;
        }

        // 뒤집기
        if (flipHorizontal) {
          x = origin.x + (columns - 1 - c) * (seatRadius * 2 + seatGap);
        }
        if (flipVertical) {
          y = origin.y + (rows - 1 - r) * (seatRadius * 2 + rowGap);
        }

        // ✅ anchor 기준 회전
        const rotated = rotatePoint(x, y);

        // 행 레이블 생성
        const rowLabel = generateLabel(r, rowLabelConfig);

        // 좌석 번호 생성
        const seatNumber = generateSeatNumber(c, seatLabelConfig);

        // 좌석 ID 생성
        const seatId = `${rowLabel}-${seatNumber}`;

        seats.push({
          seatId,
          sectionId,
          row: rowLabel,
          number: seatNumber,
          x: Math.round(rotated.x),
          y: Math.round(rotated.y),
        });
      }
    }
  });

  return seats;
}

/**
 * 렌더링용 좌석 계산 (Canvas/SVG에 그릴 때만 사용)
 * ❗ 결과는 state로 저장하지 않음
 * ✅ origin 기준 회전 (기본), anchor는 회전 모드에서만 사용
 */
export function calculateSeatsForRendering(
  seatGroup: Seat.SeatGroup,
  seatRadius: number,
  rotationAnchor?: { x: number; y: number } // ✅ 회전 모드에서만 전달
): Seat.CalculatedSeat[] {
  const seats: Seat.CalculatedSeat[] = [];
  const {
    id: groupId,
    sectionId,
    origin,
    rows,
    columns,
    seatGap,
    rowGap,
    rotation,
    flipHorizontal,
    flipVertical,
    curved,
    curveAmount,
    rowLabelConfig,
    seatLabelConfig,
  } = seatGroup;

  // ✅ anchor 기준 회전 (회전 모드) 또는 origin 기준 회전 (일반)
  const rotatePoint = (x: number, y: number): { x: number; y: number } => {
    if (rotation === 0) return { x, y };
    
    const centerX = rotationAnchor?.x ?? origin.x;
    const centerY = rotationAnchor?.y ?? origin.y;
    
    const rad = (rotation * Math.PI) / 180;
    const dx = x - centerX;
    const dy = y - centerY;
    
    return {
      x: centerX + dx * Math.cos(rad) - dy * Math.sin(rad),
      y: centerY + dx * Math.sin(rad) + dy * Math.cos(rad),
    };
  };

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < columns; c++) {
      // 좌석 위치 계산 (로컬 좌표)
      let x = origin.x + c * (seatRadius * 2 + seatGap);
      let y = origin.y + r * (seatRadius * 2 + rowGap);

      if (curved && curveAmount) {
        const progress = c / (columns - 1);
        const totalWidth = (seatRadius * 2 + seatGap) * columns - seatGap;
        const curveOffset = Math.sin(progress * Math.PI) * totalWidth * curveAmount;
        y += curveOffset;
      }

      if (flipHorizontal) {
        x = origin.x + (columns - 1 - c) * (seatRadius * 2 + seatGap);
      }
      if (flipVertical) {
        y = origin.y + (rows - 1 - r) * (seatRadius * 2 + rowGap);
      }

      // ✅ anchor 기준 회전 적용
      const rotated = rotatePoint(x, y);

      const rowLabel = generateLabel(r, rowLabelConfig);
      const seatNumber = generateSeatNumber(c, seatLabelConfig);

      seats.push({
        groupId,
        sectionId,
        row: rowLabel,
        number: seatNumber,
        x: rotated.x,
        y: rotated.y,
        radius: seatRadius,
      });
    }
  }

  return seats;
}

/**
 * 행 레이블 렌더링용 계산
 * ✅ origin 기준 회전 (기본), anchor는 회전 모드에서만 사용
 */
export function calculateRowLabelsForRendering(
  seatGroup: Seat.SeatGroup,
  seatRadius: number,
  rotationAnchor?: { x: number; y: number } // ✅ 회전 모드에서만 전달
): Seat.CalculatedRowLabel[] {
  const {
    id: groupId,
    origin,
    rows,
    rowGap,
    rowLabelConfig,
    rotation,
  } = seatGroup;

  if (!rowLabelConfig.visible) return [];

  const labels: Seat.CalculatedRowLabel[] = [];
  const labelOffset = seatRadius * 3; // 좌석으로부터의 거리

  // ✅ anchor 기준 회전 (회전 모드) 또는 origin 기준 회전 (일반)
  const rotatePoint = (x: number, y: number): { x: number; y: number } => {
    if (rotation === 0) return { x, y };
    
    const centerX = rotationAnchor?.x ?? origin.x;
    const centerY = rotationAnchor?.y ?? origin.y;
    
    const rad = (rotation * Math.PI) / 180;
    const dx = x - centerX;
    const dy = y - centerY;
    
    return {
      x: centerX + dx * Math.cos(rad) - dy * Math.sin(rad),
      y: centerY + dx * Math.sin(rad) + dy * Math.cos(rad),
    };
  };

  for (let r = 0; r < rows; r++) {
    const y = origin.y + r * (seatRadius * 2 + rowGap) + seatRadius;
    const x =
      rowLabelConfig.position === 'left'
        ? origin.x - labelOffset
        : origin.x + labelOffset;

    // ✅ anchor 기준 회전 적용
    const rotated = rotatePoint(x, y);

    labels.push({
      groupId,
      label: generateLabel(r, rowLabelConfig),
      x: rotated.x,
      y: rotated.y,
    });
  }

  return labels;
}

/**
 * 라벨 생성 (숫자 또는 알파벳)
 */
function generateLabel(
  index: number,
  config: Seat.LabelConfig
): string {
  if (config.format === 'A,B,C,D...') {
    const startCharCode = typeof config.startValue === 'string' 
      ? config.startValue.charCodeAt(0) 
      : 65; // 'A'
    return String.fromCharCode(startCharCode + index);
  }

  const startNum = typeof config.startValue === 'number' 
    ? config.startValue 
    : 1;
  return String(startNum + index);
}

/**
 * 좌석 번호 생성
 */
function generateSeatNumber(
  columnIndex: number,
  config: Seat.LabelConfig
): number {
  const startNum = typeof config.startValue === 'number' 
    ? config.startValue 
    : 1;

  if (config.direction === 'rtl') {
    // Right to Left: 역순
    return startNum + columnIndex;
  }

  return startNum + columnIndex;
}

/**
 * 좌석 그룹의 로컬 바운딩 박스 계산 (OBB용)
 * ✅ 항상 rotation = 0 기준 (로컬 좌표계)
 * ✅ 회전은 렌더링/변환 단계에서 적용
 */
export function calculateSeatGroupBounds(
  seatGroup: Seat.SeatGroup,
  seatRadius: number
): { minX: number; minY: number; maxX: number; maxY: number } {
  // ✅ 항상 rotation = 0으로 로컬 바운딩 박스만 계산
  const localGroup = { ...seatGroup, rotation: 0 };
  const seats = calculateSeatsForRendering(localGroup, seatRadius);

  if (seats.length === 0) {
    return { minX: 0, minY: 0, maxX: 0, maxY: 0 };
  }

  const minX = Math.min(...seats.map((s) => s.x - seatRadius));
  const maxX = Math.max(...seats.map((s) => s.x + seatRadius));
  const minY = Math.min(...seats.map((s) => s.y - seatRadius));
  const maxY = Math.max(...seats.map((s) => s.y + seatRadius));

  return { minX, minY, maxX, maxY };
}

/**
 * 회전 anchor 계산 (OBB의 상단 중앙 + offset)
 * ✅ 로컬 바운딩 박스의 상단 중앙을 계산 후 회전 적용
 */
export function calculateRotationAnchor(
  seatGroup: Seat.SeatGroup,
  seatRadius: number,
  offset: number = 30
): { x: number; y: number } {
  // ✅ 1. 로컬 바운딩 박스 계산 (rotation = 0 기준)
  const localBounds = calculateSeatGroupBounds(seatGroup, seatRadius);
  
  // ✅ 2. 로컬 좌표계에서 상단 중앙 + offset
  const localAnchorX = (localBounds.minX + localBounds.maxX) / 2;
  const localAnchorY = localBounds.minY - offset;
  
  // ✅ 3. 회전이 있으면 anchor를 회전
  if (seatGroup.rotation !== 0) {
    const centerX = seatGroup.origin.x;
    const centerY = seatGroup.origin.y;
    const rad = (seatGroup.rotation * Math.PI) / 180;
    
    const dx = localAnchorX - centerX;
    const dy = localAnchorY - centerY;
    
    return {
      x: centerX + dx * Math.cos(rad) - dy * Math.sin(rad),
      y: centerY + dx * Math.sin(rad) + dy * Math.cos(rad),
    };
  }
  
  return { x: localAnchorX, y: localAnchorY };
}
