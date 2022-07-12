#! /bin/sh
ssh -tt -i ~/.ssh/ssh-key-2022-06-18.key opc@192.9.249.213 tmux send-keys './run.sh' Enter C-m 
exit 0
