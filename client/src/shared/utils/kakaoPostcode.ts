type KakaoPostcodeData = {
  zonecode: string;
  address: string;
  roadAddress: string;
  jibunAddress: string;
  userSelectedType: 'R' | 'J' | string;
};

type KakaoPostcodeResult = {
  zonecode: string;
  address: string;
};

declare global {
  interface Window {
    daum?: {
      Postcode: new (options: {
        oncomplete: (data: KakaoPostcodeData) => void;
      }) => {
        open: () => void;
      };
    };
  }
}

let scriptLoadingPromise: Promise<void> | null = null;

const KAKAO_POSTCODE_SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';

const loadKakaoPostcodeScript = async () => {
  if (window.daum?.Postcode) return;

  if (!scriptLoadingPromise) {
    scriptLoadingPromise = new Promise<void>((resolve, reject) => {
      const existing = document.querySelector(`script[src="${KAKAO_POSTCODE_SCRIPT_URL}"]`) as HTMLScriptElement | null;
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error('카카오 우편번호 스크립트 로드에 실패했습니다.')), {
          once: true,
        });
        return;
      }

      const script = document.createElement('script');
      script.src = KAKAO_POSTCODE_SCRIPT_URL;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('카카오 우편번호 스크립트 로드에 실패했습니다.'));
      document.head.appendChild(script);
    });
  }

  await scriptLoadingPromise;
};

export const openKakaoPostcode = async (): Promise<KakaoPostcodeResult> => {
  await loadKakaoPostcodeScript();

  if (!window.daum?.Postcode) {
    throw new Error('카카오 우편번호 서비스를 사용할 수 없습니다.');
  }

  return new Promise<KakaoPostcodeResult>((resolve) => {
    new window.daum!.Postcode({
      oncomplete: (data: KakaoPostcodeData) => {
        const address =
          data.userSelectedType === 'R' ? (data.roadAddress || data.address) : (data.jibunAddress || data.address);

        resolve({
          zonecode: data.zonecode,
          address,
        });
      },
    }).open();
  });
};
