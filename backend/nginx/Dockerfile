FROM nginx:alpine
RUN apk add --no-cache gettext
COPY nginx.conf /etc/nginx/conf.d/default.conf.template
# ponytail: 如果已有 default.conf（如 compose.override 挂载的开发配置），跳过生成
CMD if [ -s /etc/nginx/conf.d/default.conf ]; then echo "Using existing default.conf"; else envsubst '${DOMAIN}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf; fi && nginx -g 'daemon off;'
