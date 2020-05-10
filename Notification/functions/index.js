'use strict'


const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

//cloud function that sends appropriate notifications
exports.sendNotification = functions.database.ref('/Notifications/{receiver_user_id}/{notification_id}')
.onWrite((data, context) => {
	const receiver_user_id = context.params.receiver_user_id;
	const notification_id = context.params.notification_id;

	if (!data.after.val()) {
		console.log('A notification has been deleted :' , notification_id);
		return null;
	}

	//getting the type of the notification the user is about to receive (the type will be 'request' if the user has received a friend request,
	//'message' if the user received a message and 'accepted' if the user had a friend request he sent accepted)
	admin.database().ref('/Notifications/' + receiver_user_id + '/' + notification_id + '/type').once('value').then((snapshot) => {
		const notificationType = snapshot.val();
		
		admin.database().ref('/Notifications/' + receiver_user_id + '/' + notification_id + '/from').once('value').then((snapshot) => {
			const sender_user_id = snapshot.val();
			admin.database().ref('/Users/' + sender_user_id + '/name').once('value').then((snapshot) => {
				const sender_username = snapshot.val();
				//retrieving the device token of the receiver so the notification can be sent to his device
				const DeviceToken = admin.database().ref('/Users/' + receiver_user_id + '/device_token').once('value');
				
				if (notificationType === 'request') {
					return DeviceToken.then(result => {
						const token_id = result.val();
						
						const payload = {
							notification: {
								title: "New Friend Request",
								body: sender_username + ' has sent you a friend request!',
							}
						};

						return admin.messaging().sendToDevice(token_id, payload);
					});
				} else if (notificationType === 'message') {
					
					
					//if the notification's purpose is to notify the user that he received a message, get the id of that message
					admin.database().ref('/Notifications/' + receiver_user_id + '/' + notification_id + '/message_id').once('value').then((snapshot) => {
						const message_id = snapshot.val();
						
						//getting the type of the message (whether the message is a text or an image)
						admin.database().ref('/Messages/' + receiver_user_id + '/' + sender_user_id + '/' + message_id + '/type').once('value').then((snapshot) => {
							const messageType = snapshot.val();
							
							if (messageType === 'text') {
						
								//if the message is a text, then get the text and display it in the notification alongside the sender's username
								admin.database().ref('/Messages/' + receiver_user_id + '/' + sender_user_id + '/' + message_id + '/body').once('value').then((snapshot) => {
									const message_body = snapshot.val();
									
									return DeviceToken.then(result => {
										const token_id = result.val();
										
										var payload;
										
										admin.database().ref('/Users/' + sender_user_id).once('value', function(snapshot) {
											payload = {
												notification: {
													title: sender_username,
													body: sender_username + ': ' + message_body,
												}
											};
											
											return admin.messaging().sendToDevice(token_id, payload);
										});
									});
								});
							
							} else {
								
								return DeviceToken.then(result => {
									const token_id = result.val();
									
									var payload;
									
									//if the message is an image then display in the notification that the sender sent the user an image
									admin.database().ref('/Users/' + sender_user_id).once('value', function(snapshot) {
										payload = {
											notification: {
												title: sender_username,
												body: sender_username + ' has sent you an image.'
											}
										};
										
										return admin.messaging().sendToDevice(token_id, payload);
									});
								});
							}
							
						});
					});	
					
				} else if (notificationType === 'accepted') {
					
					return DeviceToken.then(result => {
						const token_id = result.val();
						
						var payload;
						
						//if the notification's purpose is to notify the user that his friend request has been accepted, display in the notification that
						//the sender accepted his friend request
						admin.database().ref('/Users/' + sender_user_id).once('value', function(snapshot) {
							payload = {
								notification: {
									title: sender_username,
									body: sender_username + ' has accepted your friend request!'
								}
							};
							
							return admin.messaging().sendToDevice(token_id, payload);
						});
					});
				}
			});
		});
	});
		
});