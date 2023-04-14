#!/bin/sh
mvn clean package
export FNAME="$(ls target | grep lermitbot | head -1)"
scp target/$FNAME rpi:~/Documents/lermitbot
ssh -tt rpi tmux send-keys -t 0 C-c C-m
ssh -tt rpi tmux send-keys -t 0 java Space -jar Space $FNAME Enter C-m
