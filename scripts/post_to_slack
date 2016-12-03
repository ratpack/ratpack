#!/bin/bash

# Usage: post_to_slack.sh <token> <channel> <build> <scan_url> <result>

token=$1

if [[ $token == "" ]]
then
        echo "No token specified"
        exit 1
fi

shift
channel=$1
if [[ $channel == "" ]]
then
        echo "No channel specified"
        exit 1
fi

shift

build=$1

shift

url=$1

shift

result=$1

color=good
if [[ $result == "failed" ]]
then
  color=danger
fi

json="{\"channel\": \"#$channel\", \"attachments\": [{\"fallback\": \"$build\", \"color\": \"$color\", \"title\": \"$build\", \"text\": \"Scan URL: $url\"}]}"

curl -s -d "payload=$json" "https://hooks.slack.com/services/$token"
