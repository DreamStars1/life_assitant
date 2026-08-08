# 多阶段构建：pnpm + Vite 构建 → nginx 静态托管
FROM node:22-alpine AS build

RUN corepack enable && corepack prepare pnpm@11.5.1 --activate

WORKDIR /app

COPY . .

ENV HUSKY=0 CI=true
# ponytail: Docker 内 npm registry 慢时 trustPolicy 校验易超时；锁文件已入库，用 --trust-lockfile 跳过联网复核
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --ignore-scripts --trust-lockfile && \
    pnpm build:pro

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
