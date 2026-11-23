"""
Enterprise AI Platform API 통합 테스트

회원-부서 관계와 문서 CRUD 시나리오를 실제 서버에 요청해 검증합니다.
Python 표준 라이브러리만 사용하므로 별도 설치가 필요 없습니다.

사용법 (프로젝트 루트에서)
    python scripts/api_test.py
    python scripts/api_test.py --host http://localhost:8081

관리자 계정은 서버 시작 시 AdminInitializer 가 생성한 계정을 사용합니다.
(기본값: admin@enterprise.local / admin1234!, 환경변수 ADMIN_EMAIL, ADMIN_PASSWORD 로 변경)

매 실행마다 새 회원/부서/문서를 만들고, 끝나면 모두 삭제합니다.
모든 검사를 통과하면 종료 코드 0, 하나라도 실패하면 1 을 반환합니다.
"""

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

PASSWORD = "password1234"


class ApiClient:

    def __init__(self, host):
        self.host = host.rstrip("/")

    def request(self, method, path, token=None, body=None):
        headers = {}
        data = None

        if token:
            headers["Authorization"] = f"Bearer {token}"
        if body is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(body).encode("utf-8")

        req = urllib.request.Request(
            self.host + path, data=data, method=method, headers=headers)

        try:
            with urllib.request.urlopen(req, timeout=10) as res:
                status, raw = res.status, res.read()
        except urllib.error.HTTPError as e:
            status, raw = e.code, e.read()

        try:
            payload = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            payload = raw.decode("utf-8", "replace")

        return status, payload


class TestRunner:

    def __init__(self):
        self.passed = 0
        self.failed = 0

    def check(self, name, status, expected, payload=None, condition=True, reason=""):
        ok = status == expected and condition

        if ok:
            self.passed += 1
            print(f"  [PASS] {name}")
        else:
            self.failed += 1
            detail = f"기대 {expected}, 실제 {status}"
            if status == expected and reason:
                detail = reason
            print(f"  [FAIL] {name} -> {detail}")
            if payload is not None:
                print(f"         응답: {str(payload)[:200]}")

        return ok


def query(params):
    return "?" + urllib.parse.urlencode(params)


def main():
    parser = argparse.ArgumentParser(description="Enterprise AI Platform API 통합 테스트")
    parser.add_argument("--host", default=os.getenv("API_HOST", "http://localhost:8080"))
    parser.add_argument("--admin-email", default=os.getenv("ADMIN_EMAIL", "admin@enterprise.local"))
    parser.add_argument("--admin-password", default=os.getenv("ADMIN_PASSWORD", "admin1234!"))
    args = parser.parse_args()

    # Windows 콘솔에서도 한글이 깨지지 않도록 UTF-8 출력
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    api = ApiClient(args.host)
    t = TestRunner()
    run_id = str(int(time.time()))
    created = {"documents": [], "users": [], "departments": []}

    print(f"API 테스트 시작: {args.host}\n")

    try:
        # ===================== 준비 =====================
        print("[준비] 로그인, 부서, 회원")

        try:
            s, r = api.request("POST", "/api/auth/login",
                               body={"email": args.admin_email, "password": args.admin_password})
        except urllib.error.URLError as e:
            print(f"  서버에 연결할 수 없습니다: {e.reason}")
            print("  서버가 실행 중인지, --host 값이 맞는지 확인하세요.")
            return 1

        if not t.check("관리자 로그인", s, 200, r):
            print("  관리자 로그인에 실패해 중단합니다. 서버 로그의 AdminInitializer 메시지를 확인하세요.")
            return 1
        admin = r["token"]

        s, r = api.request("POST", "/api/departments", admin,
                           {"name": f"인사팀-{run_id}", "description": "인사 및 채용 담당"})
        t.check("부서 등록 (인사팀)", s, 201, r)
        dept1 = r["id"]
        created["departments"].append(dept1)

        s, r = api.request("POST", "/api/departments", admin,
                           {"name": f"재무팀-{run_id}", "description": "재무 및 예산 담당"})
        t.check("부서 등록 (재무팀)", s, 201, r)
        dept2 = r["id"]
        created["departments"].append(dept2)

        s, r = api.request("POST", "/api/users", body={
            "name": "홍길동", "email": f"hong{run_id}@test.com",
            "password": PASSWORD, "departmentId": dept1})
        t.check("회원가입 시 부서 선택 (홍길동 -> 인사팀)", s, 201, r,
                r.get("departmentId") == dept1, "부서가 지정되지 않음")
        user1, user1_email = r["id"], r["email"]
        created["users"].append(user1)

        s, r = api.request("POST", "/api/users", body={
            "name": "김철수", "email": f"kim{run_id}@test.com", "password": PASSWORD})
        t.check("회원가입 부서 없음 (김철수)", s, 201, r,
                r.get("departmentId") is None, "부서가 없어야 함")
        user2, user2_email = r["id"], r["email"]
        created["users"].append(user2)

        s, r = api.request("POST", "/api/users", body={
            "name": "없는부서", "email": f"none{run_id}@test.com",
            "password": PASSWORD, "departmentId": 999999999})
        t.check("없는 부서로 회원가입 -> 404", s, 404, r)

        _, r = api.request("POST", "/api/auth/login", body={"email": user1_email, "password": PASSWORD})
        token1 = r["token"]
        _, r = api.request("POST", "/api/auth/login", body={"email": user2_email, "password": PASSWORD})
        token2 = r["token"]

        # ===================== 회원-부서 관계 =====================
        print("\n[회원-부서 관계]")

        s, r = api.request("PUT", f"/api/users/{user2}/department", admin, {"departmentId": dept2})
        t.check("관리자가 김철수를 재무팀에 배정", s, 200, r,
                r.get("departmentId") == dept2, "배정된 부서 불일치")

        s, r = api.request("PUT", f"/api/users/{user1}/department", token1, {"departmentId": dept2})
        t.check("일반 회원이 자기 부서 변경 -> 403", s, 403, r)

        s, r = api.request("PUT", f"/api/users/{user2}/department", admin, {})
        t.check("부서 ID 없이 배정 -> 400", s, 400, r)

        s, r = api.request("PUT", f"/api/users/{user1}", token1, {
            "name": "홍길동", "email": user1_email, "departmentId": dept2})
        t.check("본인 정보 수정으로는 부서 변경 불가", s, 200, r,
                r.get("departmentId") == dept1, "부서가 바뀌면 안 됨")

        s, r = api.request("GET", f"/api/departments/{dept1}/users", token1)
        t.check("인사팀 소속 회원 목록에 홍길동 포함", s, 200, r,
                isinstance(r, list) and any(u["id"] == user1 for u in r), "홍길동 없음")

        s, r = api.request("GET", f"/api/departments/{dept1}", token1)
        t.check("부서 상세에 소속 회원 포함", s, 200, r,
                isinstance(r, dict) and len(r.get("users", [])) >= 1, "소속 회원 없음")

        s, r = api.request("GET", "/api/departments/999999999/users", token1)
        t.check("없는 부서 회원 목록 -> 404", s, 404, r)

        s, r = api.request("POST", "/api/departments", token1, {"name": f"권한없음-{run_id}"})
        t.check("일반 회원이 부서 등록 -> 403", s, 403, r)

        s, r = api.request("DELETE", f"/api/departments/{dept1}", admin)
        t.check("소속 회원이 있는 부서 삭제 -> 409", s, 409, r)

        # ===================== 문서 관리 =====================
        print("\n[문서 관리]")

        s, r = api.request("POST", "/api/documents", token1, {
            "title": "연차 휴가 신청 안내",
            "content": "연차 휴가는 그룹웨어의 휴가 신청 메뉴에서 신청하며, 부서장 승인이 필요합니다.",
            "category": "인사규정"})
        t.check("문서 등록 (부서 생략 -> 작성자 부서)", s, 201, r,
                r.get("departmentId") == dept1 and r.get("authorId") == user1,
                "작성자 또는 부서 불일치")
        doc1 = r["id"]
        created["documents"].append(doc1)

        s, r = api.request("POST", "/api/documents", token1, {"title": "타 부서 문서", "departmentId": dept2})
        t.check("일반 회원이 다른 부서로 문서 등록 -> 403", s, 403, r)

        s, r = api.request("POST", "/api/documents", admin, {
            "title": "예산 집행 지침",
            "content": "부서 예산은 분기별로 집행 계획을 제출해야 합니다.",
            "category": "재무", "departmentId": dept2})
        t.check("관리자는 다른 부서에 문서 등록 가능", s, 201, r,
                r.get("departmentId") == dept2, "부서 불일치")
        doc2 = r["id"]
        created["documents"].append(doc2)

        s, r = api.request("POST", "/api/documents", token1, {"title": " "})
        t.check("제목 없이 문서 등록 -> 400", s, 400, r)

        s, r = api.request("GET", "/api/documents")
        t.check("토큰 없이 문서 목록 -> 401", s, 401, r)

        s, r = api.request("GET", "/api/documents", token2)
        t.check("문서 목록 조회", s, 200, r,
                isinstance(r, list) and len(r) >= 2, "문서 수 부족")

        s, r = api.request("GET", "/api/documents" + query(
            {"departmentId": dept1, "category": "인사규정", "keyword": "휴가"}), token2)
        t.check("문서 검색 (부서 + 분류 + 키워드)", s, 200, r,
                isinstance(r, list) and len(r) == 1, f"검색 결과 수={len(r) if isinstance(r, list) else r}")

        s, r = api.request("GET", f"/api/documents/{doc1}", token2)
        t.check("문서 상세 조회", s, 200, r)

        s, r = api.request("GET", "/api/documents/999999999", token2)
        t.check("없는 문서 조회 -> 404", s, 404, r)

        s, r = api.request("PUT", f"/api/documents/{doc1}", token2, {"title": "변조 시도"})
        t.check("남의 문서 수정 -> 403", s, 403, r)

        time.sleep(0.01)
        s, r = api.request("PUT", f"/api/documents/{doc1}", token1, {
            "title": "연차 휴가 신청 안내 (개정)",
            "content": "연차 휴가는 사용 3일 전까지 그룹웨어에서 신청하며, 부서장 승인이 필요합니다.",
            "category": "인사규정"})
        t.check("작성자가 문서 수정 + updatedAt 갱신", s, 200, r,
                r.get("updatedAt") != r.get("createdAt"), "updatedAt 미갱신")

        s, r = api.request("PUT", f"/api/documents/{doc1}", admin, {
            "title": "연차 휴가 신청 안내 (관리자 검토)",
            "content": "연차 휴가는 사용 3일 전까지 그룹웨어에서 신청하며, 부서장 승인이 필요합니다.",
            "category": "인사규정"})
        t.check("관리자 수정 시 부서 생략 -> 기존 부서 유지", s, 200, r,
                r.get("departmentId") == dept1, "부서가 바뀌면 안 됨")

        s, r = api.request("DELETE", f"/api/documents/{doc1}", token2)
        t.check("남의 문서 삭제 -> 403", s, 403, r)

        s, r = api.request("DELETE", f"/api/users/{user1}", admin)
        t.check("문서를 작성한 회원 삭제 -> 409", s, 409, r)

        s, r = api.request("DELETE", f"/api/users/{user2}/department", admin)
        t.check("관리자가 김철수 부서 해제", s, 200, r,
                r.get("departmentId") is None, "부서가 남아 있음")

        s, r = api.request("DELETE", f"/api/departments/{dept2}", admin)
        t.check("문서가 있는 부서 삭제 -> 409", s, 409, r)

        s, r = api.request("DELETE", f"/api/documents/{doc1}", token1)
        t.check("작성자가 문서 삭제", s, 204, r)
        created["documents"].remove(doc1)

        s, r = api.request("DELETE", f"/api/documents/{doc2}", admin)
        t.check("관리자가 문서 삭제", s, 204, r)
        created["documents"].remove(doc2)

    finally:
        # ===================== 정리 =====================
        # 중간에 실패해도 만든 데이터는 최대한 삭제
        if "admin" in locals():
            for doc_id in created["documents"]:
                api.request("DELETE", f"/api/documents/{doc_id}", admin)
            for user_id in created["users"]:
                api.request("DELETE", f"/api/users/{user_id}/department", admin)
                api.request("DELETE", f"/api/users/{user_id}", admin)
            for dept_id in created["departments"]:
                api.request("DELETE", f"/api/departments/{dept_id}", admin)

    print(f"\n결과: {t.passed}개 통과, {t.failed}개 실패 (테스트 데이터 정리 완료)")
    return 0 if t.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
