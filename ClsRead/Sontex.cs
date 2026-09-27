using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using MySql.Data.MySqlClient;
using System.Threading;
using System.IO;
using System.IO.Ports;
using System.Windows.Forms;
using System.Data;

namespace TTCReading.Class.ClsRead
{
    class Sontex
    {
        ClsConnDB ConnDB = new ClsConnDB();
        ClsCRC16 CRC16 = new ClsCRC16();
        ClsComPort port = new ClsComPort();
        //SerialPort ComPort1 = new SerialPort("COM16",9600,Parity.Even,8,StopBits.Two);
        SerialPort ComPort = new SerialPort();
        string pathmeter = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location) + "\\ElectricRT\\";
        double[] datameter;
        string meterid = "";
        string meterName = "";
        string MeterAddr = "";
        string energytype = "";
        string commPort = "";
        string dis = "";
        string chkerrorcon = "";
        string str = "";
        string[] str1;
        string chkdata = "";
        string por_type = "";
        string path = @"D:\LOG_DATA_KWH.txt";
        string path1 = @"D:\LOG_DATA_BYTE.txt";
        double[] denominator = new double[35];
        string hex;
        string[] b_dis = new string[8];
        string date;
        double dec;
        double unit;

        private void setdenominator()
        {
            //ตัวหารค่าที่ได้จาก Meter
            denominator[0] = 10;//VoltP1
            denominator[1] = 10;//VoltP2
            denominator[2] = 10;//VoltP3
            denominator[3] = 10;//VoltL1
            denominator[4] = 10;//VoltL2
            denominator[5] = 10;//VoltL3
            denominator[6] = 1;//Amp1
            denominator[7] = 1;//Amp2
            denominator[8] = 1;//Amp3
            denominator[9] = 10;//kW1
            denominator[10] = 10;//kW2
            denominator[11] = 10;//kW3
            denominator[12] = 10;//THD U L1
            denominator[13] = 10;//THD U L2
            denominator[14] = 10;//THD U L3
            denominator[15] = 10;//THD I L1
            denominator[16] = 10;//THD I L2
            denominator[17] = 10;//THD I L3
            denominator[18] = 100;//Frequency
            denominator[19] = 1;//pf
            denominator[20] = 1;
            denominator[21] = 1;//AmpN
            denominator[22] = 1;//kW
            denominator[23] = 1;//kVAr
            denominator[24] = 1;//kVA
            denominator[25] = 1;//kWh
            denominator[26] = 1;
            denominator[27] = 1;
            denominator[28] = 1;//kVAhr
            denominator[29] = 1;//CT
            denominator[30] = 1;//CT/
            denominator[31] = 1;//Voltage
            denominator[32] = 1;//Voltage
            denominator[33] = 1;//VoltAvg
            denominator[34] = 1;//AmpAvg
        }

        #region WriteMeter
        public void readmeterSontex(DataTable tb)
        {
            foreach (DataRow dr in tb.Rows)
            {
                meterid = dr["meter_id"].ToString();
                meterName = dr["meter_name"].ToString();
                MeterAddr = dr["meter_addr"].ToString();
                if (MeterAddr == "3")
                    MeterAddr = dr["meter_addr"].ToString();
                energytype = dr["energy_type"].ToString();
                por_type = dr["por_type"].ToString();
                dis = dr["meter_alarm_dis"].ToString();
                switch (por_type)
                {
                    case "1":
                        port.cboPorts = dr["por_comport"].ToString();
                        port.cboBaudRate = dr["por_buadrate"].ToString();
                        port.cboDataBits = dr["por_databit"].ToString();
                        port.cboParity = dr["por_parity"].ToString();
                        port.cboStopBits = dr["por_stopbit"].ToString();
                        port.cboTypeMeter = por_type;
                        ComPort = port.connectSP();
                        break;
                    case "2":

                        break;
                    case "4":
                        port.cboPorts = dr["por_comport"].ToString();
                        port.cboBaudRate = dr["por_buadrate"].ToString();
                        port.cboDataBits = dr["por_databit"].ToString();
                        port.cboParity = dr["por_parity"].ToString();
                        port.cboStopBits = dr["por_stopbit"].ToString();
                        port.cboTypeMeter = por_type;
                        ComPort = port.connectSP();
                        commPort = dr["por_comport"].ToString();
                        break;
                }
                try
                {   //320=energy,32A=flow,334=temp,c8=energy2,190=volunm
                    int cloop = 24; // "C8"
                    Array ar = new string[5] { "c8", "334", "2C6", "320", "190" };
                    double[] MakeDword = new double[35];
                    datameter = new double[35];                    
                    int c = 0;
                    int[] bytes = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 
                                    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 
                                    2, 2, 2, 2, 2 };
                    byte[] ReData = new byte[5];
                    double ans,ans1,ans2;
                    b_dis[0] = "false";
                    b_dis[1] = "false";
                    b_dis[2] = "false";

                    for (int i = 0; i < 35; i++)
                    {
                        datameter[i] = 0;
                    }

                    foreach (string hex1 in ar)
                    {
                        hex = hex1;

                        try
                        {
                            if (hex1 == "334")
                            {
                                str = readdatameter6byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                str1 = str.Split(',');

                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = Convert.ToByte(str1[7]);
                                ReData[3] = Convert.ToByte(str1[8]);
                                ans = CRC16.Read(ReData);
                                datameter[5] = ans / 100;
                                //datameter[4] = Math.Round(CRC16.Read(ReData) / 100, 2);
                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = Convert.ToByte(str1[11]);
                                ReData[3] = Convert.ToByte(str1[12]);
                                ans = CRC16.Read(ReData);
                                datameter[0] = ans / 100;
                                //SUPERCAL531(datameter);
                            }
                        }
                        catch (Exception ex)
                        {
                        }
                        //if (hex1 == "320")
                        //{
                        //    str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                        //    str1 = str.Split(',');

                        //    //CRC16.cnt_kwh[Convert.ToByte(MeterAddr)]++;

                        //    ReData[0] = 0x00;
                        //    ReData[1] = 0x00;
                        //    ReData[2] = Convert.ToByte(str1[6]);
                        //    ReData[3] = Convert.ToByte(str1[7]);
                        //    datameter[4] = CRC16.Read(ReData);

                        //}
                        try
                        {
                            if (hex1 == "2C6")
                            {
                                str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                str1 = str.Split(',');
                                ReData[4] = Convert.ToByte(str[3]);
                                //flow
                                ReData[0] = Convert.ToByte(str1[6]);
                                ReData[1] = Convert.ToByte(str1[7]);
                                ReData[2] = Convert.ToByte(str1[4]);
                                ReData[3] = Convert.ToByte(str1[5]);

                                ans = CRC16.Read(ReData);
                                //datameter[3] = ans / 1000;
                                //ReData[0] = 0x00;
                                //ReData[1] = 0xd6;
                                //ReData[2] = 0xd6;
                                //ReData[3] = 0x41;
                                //int intvalue = (int)ReData[2];
                                //intvalue <<= 16;
                                //intvalue += (int)ReData[2+1];
                                //string str2 = (BitConverter.ToSingle(BitConverter.GetBytes(intvalue), 0)).ToString();
                                string a = BitConverter.ToString(ReData);
                                string[] b = a.Split('-');
                                string hexString = b[0] + b[1] + b[2] + b[3];
                                uint num = uint.Parse(hexString, System.Globalization.NumberStyles.AllowHexSpecifier);

                                byte[] floatVals = BitConverter.GetBytes(num);
                                float f = BitConverter.ToSingle(floatVals, 0);
                                datameter[3] = f;
                                //SUPERCAL531(datameter);
                            }
                        }
                        catch (Exception ex)
                        {
                        }

                        try
                        {
                            if (hex1 == "c8")
                            {
                                str = readdatameter20byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                str1 = str.Split(',');

                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = 0x00;
                                ReData[3] = Convert.ToByte(str1[1]);
                                unit = CRC16.Read(ReData);

                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = 0x00;
                                ReData[3] = Convert.ToByte(str1[3]);
                                dec = CRC16.Read(ReData);
                                //energy
                                ans = 0;
                                if (unit == 19)//Wh
                                {
                                    ReData[0] = Convert.ToByte(str1[18]);
                                    ReData[1] = Convert.ToByte(str1[19]);
                                    ReData[2] = Convert.ToByte(str1[16]);
                                    ReData[3] = Convert.ToByte(str1[17]);
                                    ans1 = CRC16.Read(ReData);
                                    ReData[0] = Convert.ToByte(str1[6]);
                                    ReData[1] = Convert.ToByte(str1[7]);
                                    ReData[2] = Convert.ToByte(str1[4]);
                                    ReData[3] = Convert.ToByte(str1[5]);
                                    ans2 = CRC16.Read(ReData);
                                    if(ans2>0)
                                    {
                                        ans2/=1000;
                                        ans = ans1 + ans2;
                                    }
                                    else
                                    {
                                        ans = ans1;
                                    }
                                }
                                else if (unit == 146)//Mh
                                {
                                    ReData[0] = Convert.ToByte(str1[6]);
                                    ReData[1] = Convert.ToByte(str1[7]);
                                    ReData[2] = Convert.ToByte(str1[4]);
                                    ReData[3] = Convert.ToByte(str1[5]);
                                    ans2 = CRC16.Read(ReData);
                                    if(ans2>0)
                                    {
                                        ans = ans2;
                                    }
                                    else
                                    {
                                        ans = 0;
                                    }
                                }
                                //ans = CRC16.Read(ReData);
                                //if (dec == 1) datameter[25] = (ans2 + ans1) / 10;
                                //if (dec == 2) datameter[25] = (ans2 + ans1) / 100;
                                //if (dec == 3) datameter[25] = (ans2 + ans1) / 1000;
                                if (ans != 0)
                                {
                                    if (dec == 1) datameter[25] = ans / 10;
                                    if (dec == 2) datameter[25] = ans / 100;
                                    if (dec == 3) datameter[25] = ans / 1000;
                                }
                                else
                                {
                                    datameter[25] = 0;
                                }
                                //ReData[0] = Convert.ToByte(str1[11]);
                                //ReData[1] = Convert.ToByte(str1[12]);
                                //ReData[2] = Convert.ToByte(str1[13]);
                                //ReData[3] = Convert.ToByte(str1[14]);
                                //datameter[5] = 5;// CRC16.Read(ReData);
                                //ReData[0] = Convert.ToByte(str1[15]);
                                //ReData[1] = Convert.ToByte(str1[16]);
                                //ReData[2] = Convert.ToByte(str1[17]);
                                //ReData[3] = Convert.ToByte(str1[18]);
                                //datameter[6] = 6;// CRC16.Read(ReData);

                                //bool connect = ConnDB.connect();
                                //string data ;
                                //if (connect)
                                //{
                                //    ConnDB.sql = "SELECT eg_data FROM dbrtftyrv2.eg_tt_data where meter_id = '" + meterid + "'";
                                //    ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                                //    ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                                //    data = ConnDB.reader.GetString("eg_data");
                                //    ConnDB.reader.Close();
                                //}
                                //ConnDB.clossDB();
                                //SUPERCAL531(datameter);
                            }
                        }
                        catch (Exception ex)
                        {
                        }

                        try
                        {
                            if (hex1 == "190")
                            {
                                str = readdatameter20byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                str1 = str.Split(',');
                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = 0x00;
                                ReData[3] = Convert.ToByte(str1[3]);
                                dec = CRC16.Read(ReData);
                                //volume
                                ReData[0] = Convert.ToByte(str1[6]);
                                ReData[1] = Convert.ToByte(str1[7]);
                                ReData[2] = Convert.ToByte(str1[4]);
                                ReData[3] = Convert.ToByte(str1[5]);
                                ans = CRC16.Read(ReData);
                                if (dec == 1) datameter[28] = ans / 10;
                                if (dec == 2) datameter[28] = ans / 100;
                                if (dec == 3) datameter[28] = ans / 1000;
                                //ReData[0] = Convert.ToByte(str1[11]);
                                //ReData[1] = Convert.ToByte(str1[12]);
                                //ReData[2] = Convert.ToByte(str1[13]);
                                //ReData[3] = Convert.ToByte(str1[14]);
                                //datameter[8] = 8;// CRC16.Read(ReData);
                                //ReData[0] = Convert.ToByte(str1[15]);
                                //ReData[1] = Convert.ToByte(str1[16]);
                                //ReData[2] = Convert.ToByte(str1[17]);
                                //ReData[3] = Convert.ToByte(str1[18]);
                                //datameter[9] = 9;// CRC16.Read(ReData);
                                //SUPERCAL531(datameter);
                            }
                        }
                        catch (Exception ex)
                        {
                        }
                    }

                    string cPort = commPort;
                    SUPERCAL531(datameter);
                    port.closeSP(ComPort);
                }
                catch (Exception)
                {
                    port.closeSP(ComPort);
                    //// MessageBox.Show("Close port JanitzaUMG96S" + "meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> "  ); 
                    //using (StreamWriter sw = File.AppendText(path))
                    //{
                    //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() + "Close port JanitzaUMG96S HEX=" + hex + " meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " meterName <" + meterName + "> ");
                    //}
                }
            }
        }

        #endregion WriteMeter

        #region ReadMeter
        private string readdatameter4byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x04;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x04;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            //Thread.Sleep(10);

            byte[] Alldata = new byte[13+3];
            int offset = 0;
            int remaining = Alldata.Length;
            while (remaining > 0)
            {
                try
                {
                    int read = ComPort.Read(Alldata, offset, remaining);
                    if (read <= 0) { remaining = 0; }
                    else
                    {
                        remaining -= read;
                        offset += read;
                    }
                    if (Convert.ToBoolean(dis) == true)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 0);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                        b_dis[0] = "true";
                        
                    }
                }
                catch (Exception) 
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 1);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                        b_dis[0] = "false";
                        
                    }
                    break;
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[13];
                if (Alldata[0] == (byte)addr && Alldata[1] == 0x04 && Alldata[2] == 0x08)
                {
                    crc_result = CRC16.crc16(Alldata, 11);

                    if (crc_result[0] == Alldata[11] && crc_result[1] == Alldata[12])
                    {
                        for (int c = 0; c < 13; c++)
                        {
                            ReData[c] = Alldata[c + 3];
                            valuemeter += ReData[c].ToString() + ",";
                        }
                    }
                    else
                    {
                        for (int c = 0; c < 13; c++)
                        {
                            //ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }
                }
                else
                {
                    for (int c = 0; c < 13; c++)
                    {
                        //ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }
            }
            return valuemeter;
        }
        private string readdatameter20byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x04;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x14;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            //Thread.Sleep(10);

            byte[] Alldata = new byte[45+3];
            int offset = 0;
            int remaining = Alldata.Length;
            while (remaining > 0)
            {
                try
                {
                    int read = ComPort.Read(Alldata, offset, remaining);
                    if (read <= 0) { remaining = 0; }
                    else
                    {
                        remaining -= read;
                        offset += read;
                    }
                    if (Convert.ToBoolean(dis) == true)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 0);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();

                        }
                        ConnDB.clossDB();
                        b_dis[1] = "true";
                    }
                }
                catch (Exception) 
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 1);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                        b_dis[1] = "false";
                        //AutoClosingMessageBox.Show(meterName + " --> Time out", "Meter BTU", 5000);
                    }
                    break;
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[45];
                if (Alldata[0] == (byte)addr && Alldata[1] == 0x04 && Alldata[2] == 0x28)
                {
                    crc_result = CRC16.crc16(Alldata, 43);

                    if (crc_result[0] == Alldata[43] && crc_result[1] == Alldata[44])
                    {
                        for (int c = 0; c < 45; c++)
                        {
                            ReData[c] = Alldata[c + 3];
                            valuemeter += ReData[c].ToString() + ",";
                        }
                    }
                    else
                    {
                        for (int c = 0; c < 45; c++)
                        {
                            //ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }

                }
                else
                {
                    for (int c = 0; c < 45; c++)
                    {
                        //ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }
            }
            return valuemeter;
        }
        private string readdatameter6byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];
            //ComPort1.Open();

            Data[0] = (byte)addr;
            Data[1] = 0x04;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x6;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            //Thread.Sleep(10);



            int offset = 0;
            int offset1 = 1;
            int xxxx;
            xxxx = ComPort.BytesToRead;
            byte[] Alldata = new byte[20];
            int remaining = Alldata.Length;

            while (remaining > 0)
            {
                try
                {
                    int read = ComPort.Read(Alldata, offset, remaining);
                    if (read <= 0)
                    {
                        remaining = 0;
                    }
                    else
                    {
                        remaining -= read;
                        offset += read;
                    }

                    if (Convert.ToBoolean(dis) == true)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 0);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                        b_dis[2] = "true";
                    }
                }
                catch (Exception)
                {
                    //ComPort1.Close();
                     chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id  ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 1);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                        b_dis[2] = "false";
                    }
                    break;
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[100];

                if (Alldata[0] == (byte)addr && Alldata[1] == 0x04 && Alldata[2] == 0x0c)
                {
                    crc_result = CRC16.crc16(Alldata, 15);

                    if (crc_result[0] == Alldata[15] && crc_result[1] == Alldata[16])
                    {
                        for (int c = 0; c < 17; c++)
                        {
                            ReData[c] = Alldata[c];
                            valuemeter += ReData[c].ToString() + ",";
                        }
                    }
                    else
                    {
                        for (int c = 0; c < 17; c++)
                        {
                            //ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }
                }

                else
                {
                    for (int c = 0; c < 17; c++)
                    {
                        ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }

            }
            //ComPort1.Close();
            return valuemeter;
        }
        #endregion ReadMeter

        #region Save
        private void SUPERCAL531(double[] data)
        {
            try
            {
                DataTable tb = new DataTable();
                tb.Columns.Add("Begin", typeof(string));
                tb.Columns.Add("ID", typeof(string));
                tb.Columns.Add("Name", typeof(string));
                tb.Columns.Add("kW", typeof(string));
                tb.Columns.Add("kW1", typeof(string));
                tb.Columns.Add("kW2", typeof(string));
                tb.Columns.Add("kW3", typeof(string));
                tb.Columns.Add("kVAr", typeof(string));
                tb.Columns.Add("kWh", typeof(string));
                tb.Columns.Add("kVA", typeof(string));
                tb.Columns.Add("kVArh", typeof(string));
                tb.Columns.Add("VoltP1", typeof(string));
                tb.Columns.Add("VoltP2", typeof(string));
                tb.Columns.Add("VoltP3", typeof(string));
                tb.Columns.Add("VoltAvg", typeof(string));
                tb.Columns.Add("VoltL1", typeof(string));
                tb.Columns.Add("VoltL2", typeof(string));
                tb.Columns.Add("VoltL3", typeof(string));
                tb.Columns.Add("Amp1", typeof(string));
                tb.Columns.Add("Amp2", typeof(string));
                tb.Columns.Add("Amp3", typeof(string));
                tb.Columns.Add("AmpAvg", typeof(string));
                tb.Columns.Add("AmpN", typeof(string));
                tb.Columns.Add("pf", typeof(string));
                tb.Columns.Add("Frequency", typeof(string));
                tb.Columns.Add("THDV1", typeof(string));
                tb.Columns.Add("THDV2", typeof(string));
                tb.Columns.Add("THDV3", typeof(string));
                tb.Columns.Add("THDA1", typeof(string));
                tb.Columns.Add("THDA2", typeof(string));
                tb.Columns.Add("THDA3", typeof(string));

                string id = DateTime.Now.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
                DataRow dr = tb.NewRow();
                dr["Begin"] = id;
                dr["ID"] = meterid;
                dr["Name"] = meterName;
                dr["kW"] = String.Format("{0:0.00}", data[22]);
                dr["kW1"] = String.Format("{0:0.00}", data[9]);
                dr["kW2"] = String.Format("{0:0.00}", data[10]);
                dr["kW3"] = String.Format("{0:0.00}", data[11]);
                dr["kVAr"] = String.Format("{0:0.00}", data[23]);
                dr["kWh"] = String.Format("{0:0.00}", data[25]);
                dr["kVA"] = String.Format("{0:0.00}", data[24]);
                dr["kVArh"] = String.Format("{0:0.00}", data[28]);
                dr["VoltP1"] = String.Format("{0:0.00}", data[0]);
                dr["VoltP2"] = String.Format("{0:0.00}", data[1]);
                dr["VoltP3"] = String.Format("{0:0.00}", data[2]);
                dr["VoltAvg"] = String.Format("{0:0.00}", data[33]);
                dr["VoltL1"] = String.Format("{0:0.00}", data[3]);
                dr["VoltL2"] = String.Format("{0:0.00}", data[4]);
                dr["VoltL3"] = String.Format("{0:0.00}", data[5]);
                dr["Amp1"] = String.Format("{0:0.00}", data[6]);
                dr["Amp2"] = String.Format("{0:0.00}", data[7]);
                dr["Amp3"] = String.Format("{0:0.00}", data[8]);
                dr["AmpAvg"] = String.Format("{0:0.00}", data[34]);
                dr["AmpN"] = String.Format("{0:0.00}", data[21]);
                dr["pf"] = String.Format("{0:0.00}", data[19]);
                dr["Frequency"] = String.Format("{0:0.00}", data[18]);
                dr["THDV1"] = String.Format("{0:0.00}", data[12]);
                dr["THDV2"] = String.Format("{0:0.00}", data[13]);
                dr["THDV3"] = String.Format("{0:0.00}", data[14]);
                dr["THDA1"] = String.Format("{0:0.00}", data[15]);
                dr["THDA2"] = String.Format("{0:0.00}", data[16]);
                dr["THDA3"] = String.Format("{0:0.00}", data[17]);
                tb.Rows.Add(dr);

                if (meterid == "0814")
                {
                    int x;
                }
                string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                if (b_dis[0] == "true" || b_dis[1] == "true" ||b_dis[2] == "true")
               // if (datameter[25]>0)
                {
                    double x = datameter[0];
                    SaveRealTime(dr);
                    //Save_1min(dr);
                    GC.WaitForPendingFinalizers();
                    GC.WaitForFullGCApproach();
                    GC.WaitForFullGCComplete();
                    GC.Collect();

                    bool connect = ConnDB.connect();
                    if (connect)
                    {
                        ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                        "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                        "WHERE meter_id = @meter_id ";
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 0);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                    }
                    ConnDB.clossDB();
                }
                else
                {
                    //ErrorRealTime();
                    bool connect = ConnDB.connect();
                    if (connect)
                    {
                        ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                        "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                        "WHERE meter_id = @meter_id  ";
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 1);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                    }
                    ConnDB.clossDB();
                }

            }
            catch (Exception ex) { MessageBox.Show("ReadMySQL Sontex BTU" + ex.Message); }
        }

        private bool chkrecord(string tb, string where)
        {
            bool connect = ConnDB.connect();
            bool chk = false;
            if (connect)
            {
                ConnDB.sql = "select * from " + tb + " " + where;
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                chk = ConnDB.reader.HasRows;
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
            return chk;
        }

        private void ErrorRealTime()
        {
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string MeterData = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
            bool chkdemand = chkrecord("" + ConnDB.DataDBName2 + ".eg_tt_data ", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into " + ConnDB.DataDBName2 + ".eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data);CALL `dbrtftyrv2`.`INSERT_REALTIME_RMS`(@meter_id)";
                }
                else
                {
                    ConnDB.sql = "update " + ConnDB.DataDBName2 + ".eg_tt_data " +
                    "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                    "where meter_id = @meter_id ;CALL `dbrtftyrv2`.`INSERT_REALTIME_RMS`(@meter_id) ";
                }
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                ConnDB.mycomm.Parameters.AddWithValue("@eg_data", MeterData);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
        }

        private void SaveRealTime(DataRow dr)
        {
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string MeterData = String.Format("{0:0.00}", dr["kW"].ToString()) + "," + String.Format("{0:0.00}", dr["kW1"].ToString()) + "," + String.Format("{0:0.00}", dr["kW2"].ToString()) + "," + String.Format("{0:0.00}", dr["kW3"].ToString()) + "," + String.Format("{0:0.00}", dr["kVAr"].ToString()) + "," + String.Format("{0:0.00}", dr["kWh"].ToString()) + "," + String.Format("{0:0.00}", dr["kVA"].ToString()) + "," + String.Format("{0:0.00}", dr["kVArh"].ToString()) + "," +
              String.Format("{0:0.00}", dr["VoltP1"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltP2"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltP3"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltAvg"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltL1"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltL2"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltL3"].ToString()) + "," +
              String.Format("{0:0.00}", dr["Amp1"].ToString()) + "," + String.Format("{0:0.00}", dr["Amp2"].ToString()) + "," + String.Format("{0:0.00}", dr["Amp3"].ToString()) + "," + String.Format("{0:0.00}", dr["AmpAvg"].ToString()) + "," + String.Format("{0:0.00}", dr["AmpN"].ToString()) + "," + String.Format("{0:0.00}", dr["pf"].ToString()) + "," + String.Format("{0:0.00}", dr["frequency"].ToString()) + "," + String.Format("{0:0.00}", dr["THDV1"].ToString()) + "," + String.Format("{0:0.00}", dr["THDV2"].ToString()) + "," +
              String.Format("{0:0.00}", dr["THDV3"].ToString()) + "," + String.Format("{0:0.00}", dr["THDA1"].ToString()) + "," + String.Format("{0:0.00}", dr["THDA2"].ToString()) + "," + String.Format("{0:0.00}", dr["THDA3"].ToString());
            bool chkdemand = chkrecord("" + ConnDB.DataDBName2 + ".eg_tt_data", "where meter_id ='" + meterid + "'");

            //byte[] bytes = Encoding.Default.GetBytes(MeterData);
            //MeterData = Encoding.UTF8.GetString(bytes);
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into " + ConnDB.DataDBName2 + ".eg_tt_data " +
                  "value(@meter_id, @eg_id, @energy_type, @eg_data) ;CALL `dbrtftyrv2`.`INSERT_REALTIME_RMS`(@meter_id)";
                }
                else
                {
                    ConnDB.sql = "update " + ConnDB.DataDBName2 + ".eg_tt_data " +
                     "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                     "where meter_id = @meter_id ;CALL `dbrtftyrv2`.`INSERT_REALTIME_RMS`(@meter_id)";
                   
                }

                try
                {
                    ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                    ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                    ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                    ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                    ConnDB.mycomm.Parameters.AddWithValue("@eg_data", MeterData);
                    ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                    ConnDB.reader.Close();
                }
                catch (Exception ex)
                {
                    MessageBox.Show(string.Format("JanitzaUMG96S ทดสอบค่า Meter {0} CT:{1} tt_data" + ex.Message, meterid, chkdemand));
                    ConnDB.reader.Close();
                }

            }
            ConnDB.clossDB();
        }
      
        #endregion Save
        // Or IsNanOrInfinity
        public static bool HasValue(double value)
        {
            return !Double.IsNaN(value) && !Double.IsInfinity(value);
        }

        public class AutoClosingMessageBox
        {
            System.Threading.Timer _timeoutTimer;
            string _caption;
            AutoClosingMessageBox(string text, string caption, int timeout)
            {
                _caption = caption;
                _timeoutTimer = new System.Threading.Timer(OnTimerElapsed,
                    null, timeout, System.Threading.Timeout.Infinite);
                MessageBox.Show(text, caption);
            }

            public static void Show(string text, string caption, int timeout)
            {
                new AutoClosingMessageBox(text, caption, timeout);
            }

            void OnTimerElapsed(object state)
            {
                IntPtr mbWnd = FindWindow(null, _caption);
                if (mbWnd != IntPtr.Zero)
                    SendMessage(mbWnd, WM_CLOSE, IntPtr.Zero, IntPtr.Zero);
                _timeoutTimer.Dispose();
            }
            const int WM_CLOSE = 0x0010;
            [System.Runtime.InteropServices.DllImport("user32.dll", SetLastError = true)]
            static extern IntPtr FindWindow(string lpClassName, string lpWindowName);
            [System.Runtime.InteropServices.DllImport("user32.dll", CharSet = System.Runtime.InteropServices.CharSet.Auto)]
            static extern IntPtr SendMessage(IntPtr hWnd, UInt32 Msg, IntPtr wParam, IntPtr lParam);
        }
    }
}
