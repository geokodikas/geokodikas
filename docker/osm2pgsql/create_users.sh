if [ "$(getent group "${HOST_GID}")" ]; then
    echo "Group with GID: ${HOST_GID} already exists"
else
    echo "Creating group with GID: ${HOST_GID} and name users"
    groupadd -g "${HOST_GID}" users
fi
echo "Creating user with GID: ${HOST_GID} and UID: ${HOST_UID} and name 'luser'"
useradd -u "${HOST_UID}" --gid "${HOST_GID}" luser
