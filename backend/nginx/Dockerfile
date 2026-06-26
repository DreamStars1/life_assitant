FROM nginx:alpine
RUN apk add --no-cache gettext openssl apache2-utils && \
    rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf.template
# ponytail: 首次部署时 certs 卷为空，生成自签名证书 bootstrap 让 nginx 能启动，
# acme.sh 后续会用真实证书替换；如果 compose.override 挂载了自定义配置，跳过模板生成
CMD ["/bin/sh", "-c", "\
if [ ! -f /etc/nginx/certs/fullchain.pem ]; then \
  mkdir -p /etc/nginx/certs && \
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/certs/privkey.pem \
    -out /etc/nginx/certs/fullchain.pem \
    -subj '/CN=bootstrap' && \
  echo 'Generated self-signed bootstrap cert'; \
fi && \
if [ -s /etc/nginx/conf.d/default.conf ]; then \
  echo 'Using existing default.conf'; \
else \
  envsubst '${DOMAIN}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf; \
fi && \
if [ ! -f /etc/nginx/conf.d/.htpasswd ] && [ -n \"${MONITOR_USERNAME}\" ] && [ -n \"${MONITOR_PASSWORD}\" ]; then \
  htpasswd -cb /etc/nginx/conf.d/.htpasswd \"${MONITOR_USERNAME}\" \"${MONITOR_PASSWORD}\" && \
  echo 'Generated htpasswd for monitor/logs'; \
fi && \
exec nginx -g 'daemon off;'"]
