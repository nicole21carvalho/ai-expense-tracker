# Mesma versao do CI: o Angular 22 exige Node ^22.22.3 ou ^24.15.0.
FROM node:24-alpine AS build
WORKDIR /app
# npm ci respeita o package-lock exatamente; npm install pode subir versao
# e quebrar o build so na esteira.
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/cofre-web/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
