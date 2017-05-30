package ir.parsansoft.app.ihs.center;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ir.parsansoft.app.ihs.center.adapters.AdapterFakeNodeSwitches;
import ir.parsansoft.app.ihs.center.adapters.AdapterListViewNode;

import static ir.parsansoft.app.ihs.center.G.sendCrashLog;

public class ActivityAddNode_IoMadule_NodeType extends ActivityEnhanced {

    AllViews.CO_d_section_add_node_NodeType mAdd_node_nodeType;

    private AdapterListViewNode grdListAdapter;
    private AdapterFakeNodeSwitches adapterNodeSwitches;
    private Database.Node.Struct[] devices;
    private Database.Switch.Struct[] switches;
    ArrayList<Database.Node.Struct> ioNode;
    ListView lstNode, lstNodeSwitches;
    Button btnNext, btnCancel, btnBack;
    TextView txtTitle;
    int deviceID = 0;
    int ioModuleID = 0;
    int node_type;
    List<String> availablePorts;
    private Database.Node.Struct newNode;
    private ArrayList<String> spinnerPorts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (G.setting.languageID == 1 || G.setting.languageID == 4)
            setContentView(R.layout.activity_add_node__io_madule__node_type);
        else
            setContentView(R.layout.activity_add_node__io_madule__node_type);

        setSideBarVisiblity(false);

        mAdd_node_nodeType = new AllViews.CO_d_section_add_node_NodeType(this);

        mAdd_node_nodeType.lstNode = (ListView) findViewById(R.id.lstNode);
        mAdd_node_nodeType.lstNodeSwitches = (ListView) findViewById(R.id.lstNodeSwitches);
        mAdd_node_nodeType.btnNext = (Button) findViewById(R.id.btnNext);
        mAdd_node_nodeType.btnBack = (Button) findViewById(R.id.btnBack);
        mAdd_node_nodeType.btnCancel = (Button) findViewById(R.id.btnCancel);
        mAdd_node_nodeType.txtTitle = (TextView) findViewById(R.id.txtTitle);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("IO_NODE_ID")) { // IO Module Id
                deviceID = extras.getInt("DEVICE_NODE_ID");
                ioModuleID = extras.getInt("IO_NODE_ID");
                ioNode = new ArrayList<>(Arrays.asList(Database.Node.select("iD=" + ioModuleID)));
//                devices = Database.Node.select("iD=" + deviceID);
                node_type = extras.getInt("NODE_Type");
                G.log("Node ID=" + deviceID);
            }
        }

        getAvailablePorts();

        int neededPorts = 0;

        switch (node_type) {
            case AllNodes.Node_Type.SIMPLE_SWITCH_1:
            case AllNodes.Node_Type.WC_SWITCH:
                neededPorts = 1;
                break;
            case AllNodes.Node_Type.SIMPLE_SWITCH_2:
            case AllNodes.Node_Type.CURTAIN_SWITCH:
                neededPorts = 2;
                break;
            case AllNodes.Node_Type.SIMPLE_SWITCH_3:
            case AllNodes.Node_Type.AIR_CONDITION:
                neededPorts = 3;
                break;
        }

        if (availablePorts.size() < neededPorts) {
            Intent in_out = new Intent(G.currentActivity, ActivityAddNode_IoModule_Device_Select.class);
            in_out.putExtra("IO_NODE_ID", ioNode.get(0).iD);
            G.currentActivity.startActivity(in_out);
            finish();
            Animation.doAnimation(Animation.Animation_Types.FADE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    new DialogClass(G.currentActivity).showOk(G.T.getSentence(201), G.T.getSentence(858));
                }
            }, 500);

            return;
        }

        insertFakeNodeToDb(availablePorts);
        refreshNode();

        mAdd_node_nodeType.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (saveForm()) {
                    Intent fw4 = new Intent(G.currentActivity, ActivityAddNode_IoMadule_SelectPlace.class);
                    fw4.putExtra("DEVICE_NODE_ID", deviceID);
                    fw4.putExtra("IO_NODE_ID", ioModuleID);
                    fw4.putExtra("NODE_Type", node_type);
                    G.currentActivity.startActivity(fw4);
                    Animation.doAnimation(Animation.Animation_Types.FADE_SLIDE_LEFTRIGHT_RIGHT);
                    finish();
                }
            }
        });
        mAdd_node_nodeType.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    Intent fw3 = new Intent(G.currentActivity, ActivityAddNode_IoModule_Device_Select.class);
                    fw3.putExtra("DEVICE_NODE_ID", deviceID);
                    fw3.putExtra("IO_NODE_ID", ioModuleID);
                    fw3.putExtra("Delete_Last_Node", true);
                    Animation.doAnimation(Animation.Animation_Types.FADE_SLIDE_LEFTRIGHT_LEFT);

                    G.nodeCommunication.allNodes.get(deviceID).distroyNode();
                    G.nodeCommunication.allNodes.remove(deviceID);

                    devices = new Database.Node.Struct[0];

                    Database.Node.delete(deviceID);
                    Database.Switch.delete("nodeID=" + deviceID);

                    refreshNode();

                    NetMessage netMessage = new NetMessage();
                    netMessage.data = newNode.getNodeDataJson();
                    netMessage.action = NetMessage.Delete;
                    netMessage.type = NetMessage.NodeData;
                    netMessage.typeName = NetMessage.NetMessageType.NodeData;
                    netMessage.messageID = netMessage.save();
                    G.mobileCommunication.sendMessage(netMessage);
                    G.server.sendMessage(netMessage);

                    G.currentActivity.startActivity(fw3);
                } catch (Exception e) {
                    sendCrashLog(e, "دکمه قبلی در اضافه کردن دستگاه خروجی به IO", Thread.currentThread().getStackTrace()[2]);
                }
                finish();

            }
        });
        mAdd_node_nodeType.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    Animation.doAnimation(Animation.Animation_Types.FADE);

                    G.nodeCommunication.allNodes.get(deviceID).distroyNode();
                    G.nodeCommunication.allNodes.remove(deviceID);

                    devices = new Database.Node.Struct[0];

                    Database.Node.delete(deviceID);
                    Database.Switch.delete("nodeID=" + deviceID);

                    refreshNode();

                    NetMessage netMessage = new NetMessage();
                    netMessage.data = newNode.getNodeDataJson();
                    netMessage.action = NetMessage.Delete;
                    netMessage.type = NetMessage.NodeData;
                    netMessage.typeName = NetMessage.NetMessageType.NodeData;
                    netMessage.messageID = netMessage.save();
                    G.mobileCommunication.sendMessage(netMessage);
                    G.server.sendMessage(netMessage);
                } catch (Exception e) {
                    sendCrashLog(e, "دکمه انصراف در اضافه کردن دستگاه خروجی به IO", Thread.currentThread().getStackTrace()[2]);
                }
                finish();
            }
        });
        translateForm();
    }

    private void refreshNode() {
        grdListAdapter = new AdapterListViewNode(G.currentActivity, devices, false);
        mAdd_node_nodeType.lstNode.setAdapter(grdListAdapter);
        switches = Database.Switch.select("nodeID = " + deviceID + " AND switchType not in (" + AllNodes.Switch_Type.CURTAIN_STATUS_Stop + ")");
        adapterNodeSwitches = new AdapterFakeNodeSwitches(this, switches, spinnerPorts);
        mAdd_node_nodeType.lstNodeSwitches.setAdapter(adapterNodeSwitches);
    }

    @Override
    protected void onResume() {
        super.onResume();
        G.currentActivity = this;
    }

    private List<String> getAvailablePorts() {
        try {
            availablePorts = new ArrayList<>();
            spinnerPorts = new ArrayList<>();
            availablePorts.add("3");
            availablePorts.add("4");
            availablePorts.add("5");
            availablePorts.add("6");
            availablePorts.add("7");
            availablePorts.add("8");
            availablePorts.add("9");
            availablePorts.add("10");
            availablePorts.add("11");
            availablePorts.add("12");

            Database.Node.Struct[] nodes = Database.Node.select("iP='" + ioNode.get(0).iP + "'");
            ArrayList<Database.Switch.Struct> fakeswitches = new ArrayList<>();
            for (int i = 0; i < nodes.length; i++) {
                Database.Switch.Struct[] switches = Database.Switch.select("isIOModuleSwitch=" + 1 + " AND nodeID = " + nodes[i].iD
                        + " AND switchType not in (" + AllNodes.Switch_Type.CURTAIN_STATUS_Stop + ")");

                if (switches != null) {
                    for (int j = 0; j < switches.length; j++) {
                        fakeswitches.add(switches[j]);
                    }
                }
            }

            for (int i = 0; i < fakeswitches.size(); i++) {
                availablePorts.remove(String.valueOf(fakeswitches.get(i).IOModulePort));
            }

            for (int i = 0; i < availablePorts.size(); i++) {
                spinnerPorts.add(String.valueOf(Integer.parseInt(availablePorts.get(i)) - 2));
            }

            return spinnerPorts;
        } catch (Exception e) {
            G.printStackTrace(e);

            sendCrashLog(e, "ثبت پورت در اضافه کردن دستگاه خروجی به IO", Thread.currentThread().getStackTrace()[2]);

            return null;
        }
    }

    @Override
    public void translateForm() {
        super.translateForm();
        mAdd_node_nodeType.txtTitle.setText(G.T.getSentence(226));
        mAdd_node_nodeType.btnCancel.setText(G.T.getSentence(102));
        mAdd_node_nodeType.btnBack.setText(G.T.getSentence(104));
        mAdd_node_nodeType.btnNext.setText(G.T.getSentence(103));
    }

    private void insertFakeNodeToDb(List<String> availablePorts) {

        newNode = new Database.Node.Struct();
        newNode.nodeTypeID = node_type;
//        newNode.roomID = AllNodes.myHouseDefaultRoomId;
        newNode.roomID = Database.Room.getMax("ID", "").iD;
        newNode.iP = ioNode.get(0).iP;
        deviceID = AllNodes.AddNewNode(newNode, ioModuleID, false, 0);// 4th parameter is just for sensor

        devices = new Database.Node.Struct[1];
        devices[0] = newNode;

        switches = Database.Switch.select("nodeID = " + deviceID);

        for (int i = 0; i < switches.length; i++) {
            switches[i].IOModulePort = Integer.parseInt(availablePorts.get(i));

            if (i == 2 && newNode.nodeTypeID == AllNodes.Node_Type.CURTAIN_SWITCH) {

                switches[i].IOModulePort = 0;
            }

            Database.Switch.edit(switches[i]);
        }
    }

    private boolean saveForm() {
        Database.Switch.Struct[] switchItems = adapterNodeSwitches.getSwitchItems();
        if (switchItems != null && switchItems.length > 0) {
            for (int i = 0; i < switchItems.length; i++) {
                if (switchItems[i].name.trim().length() == 0) {
                    new DialogClass(G.context).showOk(G.T.getSentence(151), G.T.getSentence(221));
                    return false;
                }
            }

            for (int i = 0; i < switchItems.length; i++) {
                switch (switchItems.length) {
                    case 1:
                        break;
                    case 2:
                        if (
                                (switchItems[0].IOModulePort == (switchItems[1].IOModulePort))
                                ) {
                            new DialogClass(G.context).showOk(G.T.getSentence(151), G.T.getSentence(248));
                            return false;
                        }
                        break;
                    case 3:
                        if (
                                (switchItems[0].IOModulePort == (switchItems[1].IOModulePort))
                                        || (switchItems[0].IOModulePort == (switchItems[2].IOModulePort))
                                        || (switchItems[1].IOModulePort == (switchItems[2].IOModulePort))
                                ) {
                            new DialogClass(G.context).showOk(G.T.getSentence(151), G.T.getSentence(248));
                            return false;
                        }
                        break;
                }
            }


            NetMessage netMessage = new NetMessage();
            netMessage.data = newNode.getNodeDataJson();
            netMessage.action = NetMessage.Insert;
            netMessage.type = NetMessage.NodeData;
            netMessage.typeName = NetMessage.NetMessageType.NodeData;
            netMessage.messageID = netMessage.save();
            G.mobileCommunication.sendMessage(netMessage);
            G.server.sendMessage(netMessage);

            //  Send message to server and local Mobiles
            NetMessage netMessage2 = new NetMessage();
            netMessage2.data = newNode.getNodeSwitchesDataJson();
            netMessage2.action = NetMessage.Insert;
            netMessage2.type = NetMessage.SwitchData;
            netMessage2.typeName = NetMessage.NetMessageType.SwitchData;
            netMessage2.messageID = netMessage2.save();
            G.mobileCommunication.sendMessage(netMessage2);
            G.server.sendMessage(netMessage2);


            NetMessage nm = new NetMessage();
            nm.action = NetMessage.Update;
            nm.type = NetMessage.SwitchData;
            JSONArray ja = new JSONArray();
            try {
                switches = Database.Switch.select("nodeID = " + deviceID);

                for (int i = 0; i < switches.length; i++) {

                    // dar node parde, IOModulePorte switch 2 yani motevaghef ro dasti 0 mikonim
                    // choon parde 3 switche shode ama faghat 2 switch ro baraye ertebat ba io niaz darim

                    JSONObject jo = new JSONObject();

                    jo.put("ID", switches[i].iD);
                    jo.put("Name", switches[i].name);
                    jo.put("Code", switches[i].code);
                    jo.put("Value", switches[i].value);
                    jo.put("NodeID", switches[i].nodeID);
                    jo.put("IOModulePort", switches[i].IOModulePort);
                    ja.put(jo);
                }
            } catch (JSONException e) {
                G.printStackTrace(e);

                sendCrashLog(e, "ساخت json object", Thread.currentThread().getStackTrace()[2]);
            }


            G.nodeCommunication.allNodes.get(newNode.iD).refreshNodeStruct();
            nm.data = ja.toString();
            nm.messageID = nm.save();
            SysLog.log("Device " + devices[0].name + " Edited.", SysLog.LogType.DATA_CHANGE, SysLog.LogOperator.NODE, deviceID);
            G.mobileCommunication.sendMessage(nm);
            G.server.sendMessage(nm);
            return true;
        } else {
            return false;
        }
    }
}
