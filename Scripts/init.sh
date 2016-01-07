#!/bin/bash

BASE_NAME=vk_test
RABBITMQ_HOST=localhost
MONGODB_HOST=localhost

#init mongo databases
mongo "$MONGODB_HOST"/"$BASE_NAME" init_mongo.js

#init redis set

#init queues
wget http://"$RABBITMQ_HOST":15672/cli/rabbitmqadmin
chmod +x rabbitmqadmin
./rabbitmqadmin declare exchange name="$BASE_NAME" type=direct durable=true

./rabbitmqadmin declare queue name="$BASE_NAME"_user_info_es durable=true
./rabbitmqadmin declare queue name="$BASE_NAME"_user_info_mongo durable=true
./rabbitmqadmin declare queue name="$BASE_NAME"_wall_posts_es durable=true
./rabbitmqadmin declare queue name="$BASE_NAME"_wall_posts_mongo durable=true

./rabbitmqadmin declare queue name="$BASE_NAME"_new_users durable=true
./rabbitmqadmin declare queue name="$BASE_NAME"_friends durable=true

./rabbitmqadmin declare binding source="$BASE_NAME" destination="$BASE_NAME"_user_info_es routing_key=user_info
./rabbitmqadmin declare binding source="$BASE_NAME" destination="$BASE_NAME"_user_info_mongo routing_key=user_info
./rabbitmqadmin declare binding source="$BASE_NAME" destination="$BASE_NAME"_wall_posts_es routing_key=wall_posts
./rabbitmqadmin declare binding source="$BASE_NAME" destination="$BASE_NAME"_wall_posts_mongo routing_key=wall_posts

./rabbitmqadmin declare binding source="$BASE_NAME" destination="$BASE_NAME"_new_users routing_key=new_users
./rabbitmqadmin declare binding source="$BASE_NAME" destination="$BASE_NAME"_friends routing_key=friends

rm ./rabbitmqadmin