package com.zacck.mapcontentobserver;

/**
 * Created by Zacck on 3/16/2016.
 */
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import android.util.Log;
import com.zacck.mapcontentobserver.BluetoothMapUtils.TYPE;

public class BluetoothMapbMessageSms extends BluetoothMapbMessage {
	private ArrayList<SmsPdu> smsBodyPdus = null;
	private String smsBody = null;
	public void setSmsBodyPdus(ArrayList<SmsPdu> smsBodyPdus) {
		this.smsBodyPdus = smsBodyPdus;
		this.charset = null;
		if(smsBodyPdus.size() > 0)
			this.encoding = smsBodyPdus.get(0).getEncodingString();
	}
	public String getSmsBody() {
		return smsBody;
	}
	public void setSmsBody(String smsBody) {
		this.smsBody = smsBody;
		this.charset = "UTF-8";
		this.encoding = null;
	}
	@Override
	public void parseMsgPart(String msgPart) {
		if(appParamCharset == BluetoothMapAppParams.CHARSET_NATIVE) {
			if(D) Log.d(TAG, "Decoding \"" + msgPart + "\" as native PDU");
			byte[] msgBytes = decodeBinary(msgPart);
			if(msgBytes.length > 0 &&
					msgBytes[0] < msgBytes.length-1 &&
					(msgBytes[msgBytes[0]+1] & 0x03) != 0x01) {
				if(D) Log.d(TAG, "Only submit PDUs are supported");
				throw new IllegalArgumentException("Only submit PDUs are supported");
			}
			smsBody += BluetoothMapSmsPdu.decodePdu(msgBytes,
					type == TYPE.SMS_CDMA ? BluetoothMapSmsPdu.SMS_TYPE_CDMA
							: BluetoothMapSmsPdu.SMS_TYPE_GSM);
		} else {
			smsBody += msgPart;
		}
	}
	@Override
	public void parseMsgInit() {
		smsBody = "";
	}
	public byte[] encode() throws UnsupportedEncodingException
	{
		ArrayList<byte[]> bodyFragments = new ArrayList<byte[]>();
        /* Store the messages in an ArrayList to be able to handle the different message types in a generic way.
         * We use byte[] since we need to extract the length in bytes.
         */
		if(smsBody != null) {
			String tmpBody = smsBody.replaceAll("END:MSG", "/END\\:MSG"); // Replace any occurrences of END:MSG with \END:MSG
			bodyFragments.add(tmpBody.getBytes("UTF-8"));
		}else if (smsBodyPdus != null && smsBodyPdus.size() > 0) {
			for (SmsPdu pdu : smsBodyPdus) {
				// This cannot(must not) contain END:MSG
				bodyFragments.add(encodeBinary(pdu.getData(),pdu.getScAddress()).getBytes("UTF-8"));
			}
		} else {
			bodyFragments.add(new byte[0]); // TODO: Is this allowed? (An empty message)
		}
		return encodeGeneric(bodyFragments);
	}
}