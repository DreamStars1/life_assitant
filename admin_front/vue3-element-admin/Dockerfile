# 多阶段构建：pnpm + Vite 构建 → nginx 静态托管
FROM node:22-alpine AS build

RUN corepack enable && corepack prepare pnpm --activate

WORKDIR /app

COPY . .

ENV HUSKY=0 CI=true
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --ignore-scripts && \
    pnpm build-only

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
