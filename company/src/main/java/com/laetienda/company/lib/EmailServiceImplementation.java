package com.laetienda.company.lib;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.company.Friend;
import com.laetienda.model.messager.EmailMessage;
import com.laetienda.utils.service.api.ApiMessenger;
import com.laetienda.utils.service.api.ApiUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashSet;

@Component
public class EmailServiceImplementation implements EmailService{
    private final static Logger log = LoggerFactory.getLogger(EmailServiceImplementation.class);

    @Autowired ApiMessenger email;
    @Autowired ApiUser apiUser;

    @Value("${webapp.messenger.enable}")
    private Boolean flag;

    @Value("${kc.client-registration-id.webapp}")
    private String clientRegistrationId;

    @Override
    public void sendFriendRequest(Friend friend) throws NotValidCustomException {
        log.debug("EMAIL_SERVICE::sendFriendRequest.");

        try {
            if(flag) {
                String buddyUserId = friend.getBuddy().getUserId();
                String emailAddress = apiUser.getEmailAddress(buddyUserId, clientRegistrationId);
                EmailMessage message = new EmailMessage("company/SendFriendRequest.html", emailAddress, "You have a new friend request");
                message.setItem(friend, Friend.class);
                email.send(message);

            }else{
                log.debug("EMAIL_SERVICE::sendFriendRequest. Messenger is disabled. $flag: {}", false);
            }

        }catch(HttpStatusCodeException e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public void acceptFriendRequest(Friend friend) throws NotValidCustomException {
        log.debug("EMAIL_SERVICE::acceptFriendRequest");

        try{
            if(flag){
                String memberUserId = friend.getMember().getUserId();
                String address = apiUser.getEmailAddress(memberUserId, clientRegistrationId);
                EmailMessage message = new EmailMessage("company/AcceptFriendRequest.html", address, "Friend request has been accepted");
                message.setItem(friend, Friend.class);
                email.send(message);
            }

        }catch(HttpStatusCodeException e){
            throw new NotValidCustomException(e);
        }
    }
}
