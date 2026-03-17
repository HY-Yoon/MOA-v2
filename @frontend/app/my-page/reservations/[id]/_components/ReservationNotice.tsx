'use client';

import SectionLayout from './SectionLayout';

const NOTICE_TEXT = `• 예매수수료는 예매일 이후 취소시에는 환불되지 않습니다.
• 이미 배송이 시작된 티켓의 경우 인터넷 및 전화로 취소할 수 없습니다. 반드시 취소마감 시간 이전에 티켓이 고객센터로 반송되어야 취소가능하며, 취소 수수료는 도착일자 기준으로 부과됩니다. 
  (단, 배송료는 환불되지 않으며 입장배송 상품의 경우 취소에 대한 자세한 문의는 고객센터로 문의해주시기 바랍니다.)
• 예매취소시점과 해당 카드사의 환불 처리기준에 따라 취소금액의 환급방법과 환급일은 다소 차이가 있을 수 있습니다. 예매 취소 시 최초 결제 동일카드로 예매 시점에 따라 취소 수수료와 배송료를 재승인합니다.
• 티켓 부분 취소 시 할부 결제는 티켓 예매 시점으로 적용됩니다. 
  (무이자할부 행사기간이 아닌 경우 혜택받지 못하실 수 있으니 유의하시기 바랍니다.) 
• 휴대폰결제로 예매하신 분은 휴대폰결제 이용료가 수수료에 함께 부과됩니다. 예매취소시는 환원됩니다.
• 기타 문의사항은 콜센터 혹은 고객센터를 이용하시기 바랍니다.
• 문화비소득공제 대상 여부는 해당 공연 또는 전시 판매자(기획사, 주최사 등)의 문화비 소득공제 사업자 등록 여부에 따릅니다.`;

/** 빨간색으로 표시할 줄 번호 (0부터 시작) */
const RED_LINE_INDICES = new Set([0]);
/** 볼드로 표시할 줄 번호 (0부터 시작) */
const BOLD_LINE_INDICES = new Set([7]);
/** 회색 글씨로 표시할 줄 번호 (0부터 시작) */
const GRAY_SMALL_LINE_INDICES = new Set<number>([2, 5]);

const NOTICE_LINES = NOTICE_TEXT.trim().split('\n');

function getLineClassName(index: number) {
  const classes: string[] = [];
  if (RED_LINE_INDICES.has(index)) classes.push('text-red-600');
  if (BOLD_LINE_INDICES.has(index)) classes.push('font-bold');
  if (GRAY_SMALL_LINE_INDICES.has(index)) classes.push('text-slate-500 text-sm');
  return classes.length ? classes.join(' ') : undefined;
}

export default function ReservationNotice() {
  return (
    <SectionLayout title="유의사항" customClass="pb-12">
      <div className="space-y-1 text-sm leading-relaxed text-slate-800">
        {NOTICE_LINES.map((line, i) => (
          <p key={i} className={getLineClassName(i)}>
            {line}
          </p>
        ))}
      </div>
    </SectionLayout>
  );
}
