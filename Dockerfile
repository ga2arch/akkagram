FROM ga2arch/nimgram-base:latest

ADD build/libs/TelegramBot-all.jar /home/akkagram/

WORKDIR /home/akkagram

ENV REDIS 127.0.0.1
RUN mkdir static

EXPOSE 8000
ENTRYPOINT ["java", "-cp", "TelegramBot-all.jar",  "Main"]