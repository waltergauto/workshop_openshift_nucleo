FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/*

COPY public/index.html /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chmod -R 755 /usr/share/nginx/html /var/cache/nginx /var/log/nginx /var/run

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]