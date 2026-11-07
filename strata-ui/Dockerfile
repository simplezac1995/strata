# syntax=docker/dockerfile:1
FROM node:24-alpine AS build
WORKDIR /web
# English-first demo: the SPA falls back to zh unless the build declares the default language.
ARG VITE_DEFAULT_LANG=en
ENV VITE_DEFAULT_LANG=${VITE_DEFAULT_LANG}
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /web/dist /usr/share/nginx/html
EXPOSE 80