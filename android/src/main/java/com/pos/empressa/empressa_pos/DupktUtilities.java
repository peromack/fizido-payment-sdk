package com.pos.empressa.empressa_pos;


import com.socsi.exception.PINPADException;
import com.socsi.exception.SDKException;
import com.socsi.smartposapi.gmalgorithm.Dukpt;
import com.socsi.utils.StringUtil;

public class DupktUtilities {

    private Dukpt dukpt = null;
    private int dukptIndex = 1;
    private String ksn = "0000000006DDDDE01500";
    private final String DukptKey = "9F8011E7E71E483B";//FE9C1B69FB3FAE71FE9C1B69FB3FAE71



    public DupktUtilities() {
        init();
    }

    private void init(){
        dukpt = Dukpt.getInstance();

        boolean isDukptKeyExist = false;
        try {
            isDukptKeyExist = dukpt.isDukptKeyExist((byte) dukptIndex);
        } catch (SDKException e) {
            e.printStackTrace();
        } catch (PINPADException e) {
            e.printStackTrace();
        }

        if (isDukptKeyExist) {
            try {
                ksn = StringUtil.byte2HexStr(dukpt.getCurrentukptKsn());
            } catch (SDKException e) {
                e.printStackTrace();
            } catch (PINPADException e) {
                e.printStackTrace();
            }
            
        }
    }


    public String loadDukpt() {
        byte[] bytes = null;
        try {
            bytes = Dukpt.getInstance().loadDukptKey((byte) 0xff, (byte) 0x01, (byte) 0x01, StringUtil.hexStr2Bytes(ksn), StringUtil.hexStr2Bytes(DukptKey), null);
        } catch (SDKException e) {
            e.printStackTrace();
        } catch (PINPADException e) {
            e.printStackTrace();
        }
        return  StringUtil.byte2HexStr(bytes) ;
    }

}
