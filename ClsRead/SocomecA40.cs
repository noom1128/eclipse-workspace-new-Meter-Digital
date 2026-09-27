using MySql.Data.MySqlClient;
using System;
using System.Collections.Generic;
using System.Data;
using System.IO;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using System.Xml;

namespace TTCReading.Class.ClsRead
{
    class SocomecA40
    {
        ClsConnDB ConnDB = new ClsConnDB();
        ClsCRC16 CRC16 = new ClsCRC16();
        ClsComPort port = new ClsComPort();
        SerialPort ComPort = new SerialPort();
        string pathmeter = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location) + "\\ElectricRT\\";
        double[] datameter;
        string meterid = "";
        string meterName = "";
        string MeterAddr = "";
        string energytype = "";
        string dis = "";
        string chkerrorcon = "";
        string str = "";
        string[] str1;
        string chkdata = "";
        string por_type = "";
        string port_id = "";
        string path = @"D:\LOG_DATA_KWH_a40_read.txt";
        string path1 = @"D:\LOG_DATA_BYTE.txt";
        string path2 = @"D:\LOG_DATA_KWH_a40_NO_read.txt";
        string path3 = @"D:\LOG_DATA_KWH_a40_cath.txt";
        double[] denominator = new double[35];
        string hex;
        int times = 0;
        string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));

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
        public void readmeterA40(DataRow dr, string strNameMeter)
        {
            datameter = new double[35];
            //foreach (DataRow dr in tb.Rows)
            //{
                meterid = dr["meter_id"].ToString();
                meterName = dr["meter_name"].ToString();
                MeterAddr = dr["meter_addr"].ToString();
                //if (ReadStatusRealTime() == true)
                //{
                //    if (strNameMeter != meterName) continue;
                //}
                energytype = dr["energy_type"].ToString();
                dis = dr["meter_alarm_dis"].ToString();
                por_type = dr["por_type"].ToString();
                port_id = dr["por_id"].ToString();

                switch (por_type)
                {
                    case "1":
                        port.cboPorts = dr["por_comport"].ToString();
                        port.cboBaudRate = dr["por_buadrate"].ToString();
                        port.cboDataBits = dr["por_databit"].ToString();
                        port.cboParity = dr["por_parity"].ToString();
                        port.cboStopBits = dr["por_stopbit"].ToString();
                        ComPort = port.connectSP();
                        break;
                    case "2":

                        break;
                    case "3":

                        break;
                }
                try
                {
                    Array ar = new string[6] { "358","300",  "318", "31e", "316", "35a" };
                    int cloop = 24; // "C8"
                    double[] MakeDword = new double[35];
                    int c = 0;
                    int[] bytes = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 
                                    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 
                                    2, 2, 2, 2, 2 };
                    byte[] ReData = new byte[4];
                    double ans;

                    for (int i = 0; i < 35; i++)
                    {
                        datameter[i] = 0;
                    }

                    foreach (string hex1 in ar)
                    {
                        if (chkerrorcon == "")
                        {
                            hex = hex1;
                            if (meterid == "0764")
                            {
                                int x;
                            }

                            try
                            {
                                if (hex1 == "300")
                                {
                                    str = readdatameter64byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                    str1 = str.Split(',');

                                    //amp
                                    ReData[0] = Convert.ToByte(str1[0]);
                                    ReData[1] = Convert.ToByte(str1[1]);
                                    ReData[2] = Convert.ToByte(str1[2]);
                                    ReData[3] = Convert.ToByte(str1[3]);
                                    ans = CRC16.Read(ReData);
                                    datameter[6] = ans / 1000;
                                    ReData[0] = Convert.ToByte(str1[4]);
                                    ReData[1] = Convert.ToByte(str1[5]);
                                    ReData[2] = Convert.ToByte(str1[6]);
                                    ReData[3] = Convert.ToByte(str1[7]);
                                    ans = CRC16.Read(ReData);
                                    datameter[7] = ans / 1000;
                                    ReData[0] = Convert.ToByte(str1[8]);
                                    ReData[1] = Convert.ToByte(str1[9]);
                                    ReData[2] = Convert.ToByte(str1[10]);
                                    ReData[3] = Convert.ToByte(str1[11]);
                                    ans = CRC16.Read(ReData);
                                    datameter[8] = ans / 1000;

                                    ReData[0] = Convert.ToByte(str1[12]);
                                    ReData[1] = Convert.ToByte(str1[13]);
                                    ReData[2] = Convert.ToByte(str1[15]);
                                    ReData[3] = Convert.ToByte(str1[15]);
                                    ans = CRC16.Read(ReData);
                                    datameter[21] = ans / 1000;
                                    //votle u
                                    ReData[0] = Convert.ToByte(str1[16]);
                                    ReData[1] = Convert.ToByte(str1[17]);
                                    ReData[2] = Convert.ToByte(str1[18]);
                                    ReData[3] = Convert.ToByte(str1[19]);
                                    ans = CRC16.Read(ReData);
                                    datameter[0] = ans / 100;
                                    ReData[0] = Convert.ToByte(str1[20]);
                                    ReData[1] = Convert.ToByte(str1[21]);
                                    ReData[2] = Convert.ToByte(str1[22]);
                                    ReData[3] = Convert.ToByte(str1[23]);
                                    ans = CRC16.Read(ReData);
                                    datameter[1] = ans / 100;
                                    ReData[0] = Convert.ToByte(str1[24]);
                                    ReData[1] = Convert.ToByte(str1[25]);
                                    ReData[2] = Convert.ToByte(str1[26]);
                                    ReData[3] = Convert.ToByte(str1[27]);
                                    ans = CRC16.Read(ReData);
                                    datameter[2] = ans / 100;
                                    //votle 
                                    ReData[0] = Convert.ToByte(str1[28]);
                                    ReData[1] = Convert.ToByte(str1[29]);
                                    ReData[2] = Convert.ToByte(str1[30]);
                                    ReData[3] = Convert.ToByte(str1[31]);
                                    ans = CRC16.Read(ReData);
                                    datameter[3] = ans / 100;
                                    ReData[0] = Convert.ToByte(str1[32]);
                                    ReData[1] = Convert.ToByte(str1[33]);
                                    ReData[2] = Convert.ToByte(str1[34]);
                                    ReData[3] = Convert.ToByte(str1[35]);
                                    ans = CRC16.Read(ReData);
                                    datameter[4] = ans / 100;
                                    ReData[0] = Convert.ToByte(str1[36]);
                                    ReData[1] = Convert.ToByte(str1[37]);
                                    ReData[2] = Convert.ToByte(str1[38]);
                                    ReData[3] = Convert.ToByte(str1[39]);
                                    ans = CRC16.Read(ReData);
                                    datameter[5] = ans / 100;
                                    // hz
                                    ReData[0] = Convert.ToByte(str1[40]);
                                    ReData[1] = Convert.ToByte(str1[41]);
                                    ReData[2] = Convert.ToByte(str1[42]);
                                    ReData[3] = Convert.ToByte(str1[43]);
                                    ans = CRC16.Read(ReData);
                                    datameter[18] = ans / 100;
                                    //pf
                                    // hz
                                    ReData[0] = Convert.ToByte(str1[56]);
                                    ReData[1] = Convert.ToByte(str1[57]);
                                    ReData[2] = Convert.ToByte(str1[58]);
                                    ReData[3] = Convert.ToByte(str1[59]);
                                    ans = CRC16.Read(ReData);
                                    datameter[19] = ans / 1000;

                                    //kw total
                                    //ReData[0] = Convert.ToByte(str1[60]);
                                    //ReData[1] = Convert.ToByte(str1[61]);
                                    //ReData[2] = Convert.ToByte(str1[62]);
                                    //ReData[3] = Convert.ToByte(str1[63]);
                                    //ans = CRC16.Read(ReData);
                                    //datameter[22] = ans / 100;                                
                                }
                            }
                            catch (Exception ex)
                            {
                            }

                            try
                            {
                                if (hex1 == "358")
                                {
                                    str = readdatameter32byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                    str1 = str.Split(',');

                                    //kwh
                                    ReData[0] = Convert.ToByte(str1[0]);
                                    ReData[1] = Convert.ToByte(str1[1]);
                                    ReData[2] = Convert.ToByte(str1[2]);
                                    ReData[3] = Convert.ToByte(str1[3]);
                                    ans = CRC16.Read(ReData);
                                    datameter[25] = ans;

                                    //kvar
                                    //ReData[0] = Convert.ToByte(str1[8]);//32
                                    //ReData[1] = Convert.ToByte(str1[9]);
                                    //ReData[2] = Convert.ToByte(str1[10]);
                                    //ReData[3] = Convert.ToByte(str1[11]);
                                    //ans = CRC16.Read(ReData);
                                    //datameter[23] = ans;

                                    //kvah
                                    //ReData[0] = Convert.ToByte(str1[64]);
                                    //ReData[1] = Convert.ToByte(str1[65]);
                                    //ReData[2] = Convert.ToByte(str1[66]);
                                    //ReData[3] = Convert.ToByte(str1[67]);
                                    //ans = CRC16.Read(ReData);
                                    //datameter[28] = ans;
                                }
                            }
                            catch (Exception ex)
                            {
                            }

                            try
                            {
                                if (hex1 == "318")
                                {
                                    str = readdatameter32byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                    str1 = str.Split(',');

                                    //kVAr total
                                    ReData[0] = Convert.ToByte(str1[0]);
                                    ReData[1] = Convert.ToByte(str1[1]);
                                    ReData[2] = Convert.ToByte(str1[2]);
                                    ReData[3] = Convert.ToByte(str1[3]);
                                    ans = CRC16.Read(ReData);
                                    datameter[23] = ans / 100;
                                    //kva total
                                    ReData[0] = Convert.ToByte(str1[4]);
                                    ReData[1] = Convert.ToByte(str1[5]);
                                    ReData[2] = Convert.ToByte(str1[6]);
                                    ReData[3] = Convert.ToByte(str1[7]);
                                    ans = CRC16.Read(ReData);
                                    datameter[24] = ans / 100;

                                    //pf total
                                    ReData[0] = Convert.ToByte(str1[8]);
                                    ReData[1] = Convert.ToByte(str1[9]);
                                    ReData[2] = Convert.ToByte(str1[10]);
                                    ReData[3] = Convert.ToByte(str1[11]);
                                    ans = CRC16.Read(ReData);
                                    datameter[19] = ans * 0.001;
                                }
                            }
                            catch (Exception ex)
                            {
                            }

                            try
                            {
                                if (hex1 == "31e")
                                {
                                    str = readdatameter32byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                    str1 = str.Split(',');

                                    //kw
                                    ReData[0] = Convert.ToByte(str1[0]);
                                    ReData[1] = Convert.ToByte(str1[1]);
                                    ReData[2] = Convert.ToByte(str1[2]);
                                    ReData[3] = Convert.ToByte(str1[3]);
                                    ans = CRC16.Read(ReData);
                                    datameter[9] = ans / 100;

                                    ReData[0] = Convert.ToByte(str1[4]);
                                    ReData[1] = Convert.ToByte(str1[5]);
                                    ReData[2] = Convert.ToByte(str1[6]);
                                    ReData[3] = Convert.ToByte(str1[7]);
                                    ans = CRC16.Read(ReData);
                                    datameter[10] = ans / 100;
                                    ReData[0] = Convert.ToByte(str1[8]);
                                    ReData[1] = Convert.ToByte(str1[9]);
                                    ReData[2] = Convert.ToByte(str1[10]);
                                    ReData[3] = Convert.ToByte(str1[11]);
                                    ans = CRC16.Read(ReData);
                                    datameter[11] = ans / 100;
                                }
                            }
                            catch (Exception ex)
                            {
                            }

                            try
                            {
                                if (hex1 == "316")
                                {
                                    str = readdatameter2byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                    str1 = str.Split(',');

                                    //total kw
                                    ReData[0] = Convert.ToByte(str1[0]);
                                    ReData[1] = Convert.ToByte(str1[1]);
                                    ReData[2] = Convert.ToByte(str1[2]);
                                    ReData[3] = Convert.ToByte(str1[3]);
                                    ans = CRC16.Read(ReData);
                                    datameter[22] = ans / 100;
                                }
                            }
                            catch (Exception ex)
                            {
                            }

                            try
                            {
                                if (hex1 == "35a")
                                {
                                    str = readdatameter2byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                                    str1 = str.Split(',');

                                    //kvarh
                                    ReData[0] = Convert.ToByte(str1[0]);
                                    ReData[1] = Convert.ToByte(str1[1]);
                                    ReData[2] = Convert.ToByte(str1[2]);
                                    ReData[3] = Convert.ToByte(str1[3]);
                                    ans = CRC16.Read(ReData);
                                    datameter[28] = ans;
                                }
                            }
                            catch (Exception ex)
                            {
                            }
                            //datameter[12] = 0;
                        }

                    }
                    if (meterid == "0764")
                    {
                        int x;
                    }
                    A40(datameter);
                    port.closeSP(ComPort);
                }
                catch (Exception)
                {
                    port.closeSP(ComPort);
                     //MessageBox.Show("Close port Socomec A40" + "meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> "  ); 
                    //using (StreamWriter sw = File.AppendText(path3))
                    //{
                    //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() + "Close port A40 HEX=" + hex + " meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " meterName <" + meterName + "> ");
                    //}
                }
            //}
        }
        #endregion WriteMeter

        #region ReadMeter
        private string readdatameter2byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x03;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x02;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            Thread.Sleep(times);

            byte[] Alldata = new byte[12];
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
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                }
                catch (Exception) 
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                    }
                    break; 
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[12];
                if (Alldata[0] == (byte)addr && Alldata[1] == 0x03 && Alldata[2] == 0x04)
                {
                    crc_result = CRC16.crc16(Alldata, 7);

                    if (crc_result[0] == Alldata[7] && crc_result[1] == Alldata[8])
                    {
                        for (int c = 0; c < 9; c++)
                        {
                            ReData[c] = Alldata[c + 3];
                            valuemeter += ReData[c].ToString() + ",";
                        }
                    }
                    else
                    {
                        for (int c = 0; c < 9; c++)
                        {
                            //ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }
                }
                else
                {
                    for (int c = 0; c < 9; c++)
                    {
                        //ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }
            }
            return valuemeter;
        }
        private string readdatameter4byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x03;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x04;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            Thread.Sleep(times);

            byte[] Alldata = new byte[13];
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
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                }
                catch (Exception) 
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                    }
                    break; 
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[8];
                //if (meterid == "0236" || meterid == "0332" || meterid == "0334" || meterid == "0335" || meterid == "0336")
                //{
                //    //MessageBox.Show( " meter_id = " + meterid + " ADDRESS + <" + MeterAddr + ">" + " <" + meterName + "> " + valuemeter);
                //    valuemeter = "hex=" + hex + " meter_id = " + meterid + " ADDRESS + <" + MeterAddr + ">" + " <" + meterName + "> ";
                //    for (int c = 0; c < 100; c++)
                //    {
                //        ReData[c] = Alldata[c + 3];
                //        valuemeter += ReData[c].ToString() + ",";
                //    }
                //}
                if (Alldata[0] == (byte)addr && Alldata[1] == 0x03 && Alldata[2] == 0x08)
                {
                    crc_result = CRC16.crc16(Alldata, 6);

                    if (crc_result[0] == Alldata[6] && crc_result[1] == Alldata[7])
                    {
                        for (int c = 0; c < 8; c++)
                        {
                            ReData[c] = Alldata[c + 3];
                            valuemeter += ReData[c].ToString() + ",";
                        }
                    }
                    else
                    {
                        for (int c = 0; c < 9; c++)
                        {
                            //ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }
                }
                else
                {
                    for (int c = 0; c < 8; c++)
                    {
                        //ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }
            }
            return valuemeter;
        }
        private string readdatameter12byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x03;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x06;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            Thread.Sleep(times);

            byte[] Alldata = new byte[33];
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
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                }
                catch (Exception) 
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                    }
                    break; 
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[33];
                if (Alldata[0] == (byte)addr && Alldata[1] == 0x03 && Alldata[2] == 0x08)
                {
                    for (int c = 0; c < 30; c++)
                    {
                        ReData[c] = Alldata[c + 3];
                        valuemeter += ReData[c].ToString() + ",";
                    }
                }
                else
                {
                    for (int c = 0; c < 30; c++)
                    {
                        //ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }
            }
            return valuemeter;
        }
        private string readdatameter32byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];
            int[] check_sum_crc = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x03;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x32;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            Thread.Sleep(times);

            int bytes = 123;//ComPort.BytesToRead;
            byte[] Alldata = new byte[bytes + 3];

            int offset = 0;
            int remaining = Alldata.Length - 3;
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
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                        //using (StreamWriter sw = File.AppendText(path))
                        //{
                        //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() + "A40 meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> " + " kwh = " + datameter[25].ToString());
                        //}
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
                            ConnDB.sql = "UPDATE eg_ms_meter " +
                            "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 1);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                            //using (StreamWriter sw = File.AppendText(path2))
                            //{
                            //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() + "A40 meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> " + " kwh = " + datameter[25].ToString());
                            //}
                        }
                        ConnDB.clossDB();
                    }
                    break; 
                }
            }
            //if (Alldata[0] == (byte)addr && Alldata[1] == 3 && Alldata[2] == 100)
            //{
                if (chkdata == "")
                {
                    byte[] ReData = new byte[100];
                    if (Alldata[0] == (byte)addr && Alldata[1] == 0x03 && Alldata[2] == 0x64)
                    {
                         crc_result = CRC16.crc16(Alldata, 103);

                         if (crc_result[0] == Alldata[103] && crc_result[1] == Alldata[104])
                         {
                             for (int c = 0; c < 73; c++)
                             {
                                 ReData[c] = Alldata[c + 3];
                                 valuemeter += ReData[c].ToString() + ",";
                             }
                         }
                         else
                         {
                             for (int c = 0; c < 73; c++)
                             {
                                 //ReData[c] = Alldata[c + 3];
                                 valuemeter += "0" + ",";
                             }
                         }
                    }
                    else
                    {
                        for (int c = 0; c < 73; c++)
                        {
                            //ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }
                }
            //}
            return valuemeter;
        }
        private string readdatameter64byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];
            int[] check_sum_crc = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x03;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x64;

            crc_result = CRC16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            Thread.Sleep(times);

            int bytes = ComPort.BytesToRead;
            byte[] Alldata = new byte[208];

            int offset = 0;
            int offset1 = 1;
            int xxxx;
            int remaining =205;
            //int bytes = ComPort.BytesToRead;  //find the size of the array needed
            //] Alldata = new byte[bytes];  //create the array
            if (meterid == "0611")
            {
                string xx = bytes.ToString();
            }
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

                    // ComPort.Read(Alldata, 0, bytes);  //read the message and save it into the buffer
                    if (Convert.ToBoolean(dis) == true)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                }
                catch (Exception)
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
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
                        
                    }
                    //port.closeSP(ComPort);
                    break;
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[208];

                if (Alldata[0] == (byte)addr && Alldata[1] == 3 && Alldata[2] == 200)
                {
                    check_sum_crc = CRC16.crc16(Alldata, 203);

                    if (check_sum_crc[0] == Alldata[203] && check_sum_crc[1] == Alldata[204])
                    {
                        for (int c = 0; c < 205; c++)
                        {
                            ReData[c] = Alldata[c + 3];
                            valuemeter += ReData[c].ToString() + ",";
                        }
                    }
                    else
                    {
                        for (int c = 0; c < 208; c++)
                        {
                            ReData[c] = Alldata[c + 3];
                            valuemeter += "0" + ",";
                        }
                    }
                }

                else
                {
                    for (int c = 0; c < 208; c++)
                    {
                        ReData[c] = Alldata[c + 3];
                        valuemeter += "0" + ",";
                    }
                }

            }
            return valuemeter;
        }
        #endregion ReadMeter

        #region Save
        private void A40(double[] data)
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
                if (meterid == "0764")
                {
                    int x;
                }



                //if (datameter[22] != 0)
                //{

                    SaveRealTime(dr);
                    //Save_1min(dr);
                    GC.WaitForPendingFinalizers();
                    GC.WaitForFullGCApproach();
                    GC.WaitForFullGCComplete();
                    GC.Collect();

                //    bool connect = ConnDB.connect();
                //    if (connect)
                //    {
                //        ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                //        "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                //        "WHERE meter_id = @meter_id ";
                //        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 0);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                //        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                //        ConnDB.reader.Close();
                //    }
                //    ConnDB.clossDB();
                //}
                //else
                //{
                //    //ErrorRealTime();
                //    bool connect = ConnDB.connect();
                //    if (connect)
                //    {
                //        ConnDB.sql = "UPDATE " + ConnDB.DataDBName + ".eg_ms_meter " +
                //        "SET meter_alarm_dis = @meter_alarm_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                //        "WHERE meter_id = @meter_id ";
                //        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_alarm_dis", 1);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                //        ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                //        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                //        ConnDB.reader.Close();
                //    }
                //    ConnDB.clossDB();
                //}
            }
            catch (Exception ex) {
                //MessageBox.Show("ReadMySQL SocomexA40" + ex.Message);  //nop
                //ErrorRealTime();
            }
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
            bool chkdemand = chkrecord("" + ConnDB.DataDBName2 + "eg_tt_data", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into " + ConnDB.DataDBName2 + "eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                }
                else
                {
                    ConnDB.sql = "update " + ConnDB.DataDBName2 + "eg_tt_data " +
                    "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                    "where meter_id = @meter_id ";
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
            string Time = DateTime.Now.ToString("yyyyMMdd-HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string MeterData = String.Format("{0:0.00}", dr["kW"].ToString()) + "," + String.Format("{0:0.00}", dr["kW1"].ToString()) + "," + String.Format("{0:0.00}", dr["kW2"].ToString()) + "," + String.Format("{0:0.00}", dr["kW3"].ToString()) + "," + String.Format("{0:0.00}", dr["kVAr"].ToString()) + "," + String.Format("{0:0.00}", dr["kWh"].ToString()) + "," + String.Format("{0:0.00}", dr["kVA"].ToString()) + "," + String.Format("{0:0.00}", dr["kVArh"].ToString()) + "," +
              String.Format("{0:0.00}", dr["VoltP1"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltP2"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltP3"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltAvg"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltL1"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltL2"].ToString()) + "," + String.Format("{0:0.00}", dr["VoltL3"].ToString()) + "," +
              String.Format("{0:0.00}", dr["Amp1"].ToString()) + "," + String.Format("{0:0.00}", dr["Amp2"].ToString()) + "," + String.Format("{0:0.00}", dr["Amp3"].ToString()) + "," + String.Format("{0:0.00}", dr["AmpAvg"].ToString()) + "," + String.Format("{0:0.00}", dr["AmpN"].ToString()) + "," + String.Format("{0:0.00}", dr["pf"].ToString()) + "," + String.Format("{0:0.00}", dr["frequency"].ToString()) + "," + String.Format("{0:0.00}", dr["THDV1"].ToString()) + "," + String.Format("{0:0.00}", dr["THDV2"].ToString()) + "," +
              String.Format("{0:0.00}", dr["THDV3"].ToString()) + "," + String.Format("{0:0.00}", dr["THDA1"].ToString()) + "," + String.Format("{0:0.00}", dr["THDA2"].ToString()) + "," + String.Format("{0:0.00}", dr["THDA3"].ToString());
            bool chkdemand = chkrecord("" + ConnDB.DataDBName2 + ".eg_tt_data ", "where meter_id ='" + meterid + "'");

            //byte[] bytes = Encoding.Default.GetBytes(MeterData);
            //MeterData = Encoding.UTF8.GetString(bytes);
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into " + ConnDB.DataDBName2 + ".eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                }
                else
                {
                    ConnDB.sql = "update " + ConnDB.DataDBName2 + ".eg_tt_data " +
                    "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                    "where meter_id = @meter_id ";
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
                    //MessageBox.Show(string.Format("Socomec A40 ทดสอบค่า Meter {0} CT:{1} tt_data" + ex.Message, meterid, chkdemand)); //nop
                    ConnDB.reader.Close();
                }

            }
            ConnDB.clossDB();
        }

        private void XMLA40()
        {
            if (File.Exists(pathmeter + meterName + ".xml") == false)
            {
                XmlTextWriter writer = new XmlTextWriter(pathmeter + meterName + ".xml", System.Text.Encoding.UTF8);
                writer.WriteStartDocument(true);
                writer.Formatting = Formatting.Indented;
                writer.Indentation = 2;
                writer.WriteStartElement("ReadMeter");
                A40(writer);
                writer.WriteEndElement();
                writer.WriteEndDocument();
                writer.Close();
            }
            else { UpdateA40(); }
        }

        private void A40(XmlTextWriter writer)
        {
            try
            {
                double[] data = datameter;
                writer.WriteStartElement("DateTime");
                writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                writer.WriteEndElement();
                writer.WriteStartElement("MeterID");
                writer.WriteString(meterid);
                writer.WriteEndElement();
                writer.WriteStartElement("Name");
                writer.WriteString(meterName);
                writer.WriteEndElement();
                writer.WriteStartElement("Address");
                writer.WriteString(MeterAddr);
                writer.WriteEndElement();
                writer.WriteStartElement("EnergyType");
                writer.WriteString(energytype);
                writer.WriteEndElement();
                writer.WriteStartElement("kW");
                writer.WriteString(data[22].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kW1");
                writer.WriteString(data[9].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kW2");
                writer.WriteString(data[10].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kW3");
                writer.WriteString(data[11].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kVAr");
                writer.WriteString(data[23].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kWh");
                writer.WriteString(data[25].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kVA");
                writer.WriteString(data[24].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kVArh");
                writer.WriteString(data[28].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltP1");
                writer.WriteString(data[0].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltP2");
                writer.WriteString(data[1].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltP3");
                writer.WriteString(data[2].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltAvg");
                writer.WriteString(data[33].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltL1");
                writer.WriteString(data[3].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltL2");
                writer.WriteString(data[4].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltL3");
                writer.WriteString(data[5].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Amp1");
                writer.WriteString(data[6].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Amp2");
                writer.WriteString(data[7].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Amp3");
                writer.WriteString(data[8].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("AmpAvg");
                writer.WriteString(data[34].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("AmpN");
                writer.WriteString(data[21].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Pf");
                writer.WriteString(data[19].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Frequency");
                writer.WriteString(data[18].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("THDV1");
                writer.WriteString(data[12].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("THDV2");
                writer.WriteString(data[13].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("THDV3");
                writer.WriteString(data[14].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("THDI1");
                writer.WriteString(data[15].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("THDI2");
                writer.WriteString(data[16].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("THDI3");
                writer.WriteString(data[17].ToString("0.00"));
                writer.WriteEndElement();
            }
            catch (Exception e) {
               // MessageBox.Show("ReadXML !!" + e.Message);  //nop
            }
        }

        private void UpdateA40()
        {
            try
            {
                double[] data = datameter;
                string pathdatameter = pathmeter + meterName + ".xml";
                DataSet ds = new DataSet();
                ds.ReadXml(pathdatameter);
                ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                ds.Tables[0].Rows[0]["MeterID"] = meterid;
                ds.Tables[0].Rows[0]["Name"] = meterName;
                ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                ds.Tables[0].Rows[0]["kW"] = data[22].ToString("0.00");
                ds.Tables[0].Rows[0]["kW1"] = data[9].ToString("0.00");
                ds.Tables[0].Rows[0]["kW2"] = data[10].ToString("0.00");
                ds.Tables[0].Rows[0]["kW3"] = data[11].ToString("0.00");
                ds.Tables[0].Rows[0]["kVAr"] = data[23].ToString("0.00");
                ds.Tables[0].Rows[0]["kWh"] = data[25].ToString("0.00");
                ds.Tables[0].Rows[0]["kVA"] = data[24].ToString("0.00");
                ds.Tables[0].Rows[0]["kVArh"] = data[28].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltP1"] = data[0].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltP2"] = data[1].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltP3"] = data[2].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltAvg"] = data[33].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltL1"] = data[3].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltL2"] = data[4].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltL3"] = data[5].ToString("0.00");
                ds.Tables[0].Rows[0]["Amp1"] = data[6].ToString("0.00");
                ds.Tables[0].Rows[0]["Amp2"] = data[7].ToString("0.00");
                ds.Tables[0].Rows[0]["Amp3"] = data[8].ToString("0.00");
                ds.Tables[0].Rows[0]["AmpAvg"] = data[34].ToString("0.00");
                ds.Tables[0].Rows[0]["AmpN"] = data[21].ToString("0.00");
                ds.Tables[0].Rows[0]["Pf"] = data[18].ToString("0.00");
                ds.Tables[0].Rows[0]["Frequency"] = data[19].ToString("0.00");
                ds.Tables[0].Rows[0]["THDV1"] = data[12].ToString("0.00");
                ds.Tables[0].Rows[0]["THDV2"] = data[13].ToString("0.00");
                ds.Tables[0].Rows[0]["THDV3"] = data[14].ToString("0.00");
                ds.Tables[0].Rows[0]["THDI1"] = data[15].ToString("0.00");
                ds.Tables[0].Rows[0]["THDI2"] = data[16].ToString("0.00");
                ds.Tables[0].Rows[0]["THDI3"] = data[17].ToString("0.00");
                ds.WriteXml(pathdatameter);
            }
            catch (Exception) { }
        }
        #endregion Save
        // Or IsNanOrInfinity
        public static bool HasValue(double value)
        {
            return !Double.IsNaN(value) && !Double.IsInfinity(value);
        }

        private void Save_1min(DataRow dr)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, now.Minute, 0);
            string Time = date.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = date.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("" + ConnDB.DataDBName2 + ".eg_tr_minutes", "where hour_id ='" + Time + "' and meter_id ='" + meterid + "'");

            string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
            string[] eg = new string[60];
            eg[00] = value; eg[01] = value; eg[02] = value; eg[03] = value; eg[04] = value; eg[05] = value; eg[06] = value; eg[07] = value; eg[08] = value; eg[09] = value;
            eg[10] = value; eg[11] = value; eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value; eg[18] = value; eg[19] = value;
            eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value; eg[24] = value; eg[25] = value; eg[26] = value; eg[27] = value; eg[28] = value; eg[29] = value;
            eg[30] = value; eg[31] = value; eg[32] = value; eg[33] = value; eg[34] = value; eg[35] = value; eg[36] = value; eg[37] = value; eg[38] = value; eg[39] = value;
            eg[40] = value; eg[41] = value; eg[42] = value; eg[43] = value; eg[44] = value; eg[45] = value; eg[46] = value; eg[47] = value; eg[48] = value; eg[49] = value;
            eg[50] = value; eg[51] = value; eg[52] = value; eg[53] = value; eg[54] = value; eg[55] = value; eg[56] = value; eg[57] = value; eg[58] = value; eg[59] = value;

            string MeterData = dr["kW"] + "," + dr["kW1"] + "," + dr["kW2"] + "," + dr["kW3"] + "," + dr["kVAr"] + "," + dr["kWh"] + "," + dr["kVA"] + "," + dr["kVArh"] + "," +
              dr["VoltP1"] + "," + dr["VoltP2"] + "," + dr["VoltP3"] + "," + dr["VoltAvg"] + "," + dr["VoltL1"] + "," + dr["VoltL2"] + "," + dr["VoltL3"] + "," +
              dr["Amp1"] + "," + dr["Amp2"] + "," + dr["Amp3"] + "," + dr["AmpAvg"] + "," + dr["AmpN"] + "," + dr["pf"] + "," + dr["frequency"] + "," + dr["THDV1"] + "," + dr["THDV2"] + "," +
              dr["THDV3"] + "," + dr["THDA1"] + "," + dr["THDA2"] + "," + dr["THDA3"];
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into " + ConnDB.DataDBName2 + ".eg_tr_minutes " +
                    "values (@hour_id,@meter_id,@energy_type,'" +
                    eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                    eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                    eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                    eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                    eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                    eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                }
                else
                {
                    ConnDB.sql = "update " + ConnDB.DataDBName + ".eg_tr_minutes set eg_" + mm + "='" + MeterData + "' where hour_id=@hour_id and meter_id=@meter_id ";
                }
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
        }

        public Boolean ReadStatusRealTime()
        {
            Boolean output;
            // Example #1
            // Read the file as one string.
            string text = System.IO.File.ReadAllText(@"C:\WriteText.txt");
            if (text == "1\r\n")
            {
                output = true;
            }
            else
            {
                output = false;
            }
            return output;
        }
    }
}

