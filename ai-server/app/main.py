
import httpx

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(
    title="Enterprise AI Platform",
    description="기업용 AI 플랫폼 Python 서버",
    version="1.0.0"
)

# Ollama 설정
OLLAMA_CHAT_URL = "http://localhost:11434/api/generate"
OLLAMA_EMBED_URL = "http://localhost:11434/api/embed"

# AI 모델 설정
CHAT_MODEL = "gemma4:e4b"
EMBED_MODEL = "bge-m3:latest"


# 질문 요청 형식
class ChatRequest(BaseModel):
    question: str

# RAG 질문 요청 형식
class RagChatRequest(BaseModel):
    question: str
    context: str

# 임베딩 요청 형식
class EmbedRequest(BaseModel):
    text: str


# 기본 접속 확인
@app.get("/")
def home():
    return {
        "message": "Enterprise AI Platform AI Server",
        "status": "running"
    }


# 서버 상태 확인
@app.get("/health")
def health_check():
    return {
        "status": "healthy"
    }


# AI 질문 API
@app.post("/chat")
async def chat(request: ChatRequest):

    payload = {
        "model": CHAT_MODEL,
        "prompt": request.question,
        "stream": False
    }

    try:
        async with httpx.AsyncClient(timeout=180.0) as client:
            response = await client.post(
                OLLAMA_CHAT_URL,
                json=payload
            )

            response.raise_for_status()
            result = response.json()

        return {
            "question": request.question,
            "answer": result["response"],
            "model": CHAT_MODEL
        }

    except httpx.TimeoutException:
        raise HTTPException(
            status_code=504,
            detail="AI 모델 응답 시간이 초과되었습니다."
        )

    except httpx.HTTPStatusError as e:
        raise HTTPException(
            status_code=502,
            detail=f"Ollama API 오류: {e.response.text}"
        )

    except httpx.RequestError:
        raise HTTPException(
            status_code=503,
            detail="Ollama 서버에 연결할 수 없습니다."
        )

# RAG 기반 AI 질문 API
@app.post("/rag-chat")
async def rag_chat(request: RagChatRequest):

    prompt = f"""
당신은 사내 문서 검색을 도와주는 AI 어시스턴트입니다.

다음 [문서 내용]만을 근거로 사용자의 질문에 답변하세요.

규칙:
1. 문서에 명시된 내용만 근거로 답변하세요.
2. 문서에 없는 내용은 추측하거나 만들어내지 마세요.
3. 답변을 찾을 수 없다면
   "제공된 문서에서 해당 내용을 확인할 수 없습니다."라고 답변하세요.
4. 이해하기 쉽고 자연스러운 한국어로 답변하세요.
5. 답변에 사용한 근거 문장 뒤에 해당 문서의 출처를 표시하세요.
6. 출처는 문서 내용에 제공된 청크 ID를 그대로 사용하세요.
7. 제공되지 않은 청크 ID를 만들어내지 마세요.
8. 각 문장의 근거가 되는 청크 ID를 선택하세요.
9. 근거를 찾을 수 없는 문장에는 출처를 임의로 붙이지 마세요.

출처 표시 형식:
[출처: 청크 ID 11]

[문서 내용]
{request.context}

[사용자 질문]
{request.question}

[답변]
"""

    payload = {
        "model": CHAT_MODEL,
        "prompt": prompt,
        "stream": False
    }

    try:
        async with httpx.AsyncClient(timeout=180.0) as client:
            response = await client.post(
                OLLAMA_CHAT_URL,
                json=payload
            )

            response.raise_for_status()
            result = response.json()

        return {
            "question": request.question,
            "answer": result["response"],
            "model": CHAT_MODEL
        }

    except httpx.TimeoutException:
        raise HTTPException(
            status_code=504,
            detail="AI 모델 응답 시간이 초과되었습니다."
        )

    except httpx.HTTPStatusError as e:
        raise HTTPException(
            status_code=502,
            detail=f"Ollama API 오류: {e.response.text}"
        )

    except httpx.RequestError:
        raise HTTPException(
            status_code=503,
            detail="Ollama 서버에 연결할 수 없습니다."
        )


# 문장 임베딩 API
@app.post("/embed")
async def embed(request: EmbedRequest):

    if not request.text.strip():
        raise HTTPException(
            status_code=400,
            detail="임베딩할 텍스트를 입력해주세요."
        )

    payload = {
        "model": EMBED_MODEL,
        "input": request.text
    }

    try:
        async with httpx.AsyncClient(timeout=180.0) as client:
            response = await client.post(
                OLLAMA_EMBED_URL,
                json=payload
            )

            response.raise_for_status()
            result = response.json()

        embeddings = result["embeddings"]

        if not embeddings:
            raise HTTPException(
                status_code=502,
                detail="임베딩 결과가 비어 있습니다."
            )

        vector = embeddings[0]

        return {
            "model": EMBED_MODEL,
            "dimension": len(vector),
            "embedding": vector
        }

    except HTTPException:
        raise

    except httpx.TimeoutException:
        raise HTTPException(
            status_code=504,
            detail="임베딩 처리 시간이 초과되었습니다."
        )

    except httpx.HTTPStatusError as e:
        raise HTTPException(
            status_code=502,
            detail=f"Ollama 임베딩 API 오류: {e.response.text}"
        )

    except httpx.RequestError:
        raise HTTPException(
            status_code=503,
            detail="Ollama 서버에 연결할 수 없습니다."
        )
