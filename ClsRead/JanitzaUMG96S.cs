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
    class JanitzaUMG96S
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
        string path = @"D:\LOG_DATA_KWH.txt";
        string path1 = @"D:\LOG_DATA_BYTE.txt";
        double[] denominator = new double[35];
        string hex;
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
        public void readmeterUMG96S(DataTable tb)
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
                    Array ar = new string[4] {"C8" ,"1A2", "1A0", "10D" };
                    int cloop = 24; // "C8"
                    double[] MakeDword = new double[35];
                    int c = 0;
                    int[] bytes = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 
                                    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 
                                    2, 2, 2, 2, 2 };
                    byte[] ReData = new byte[4];

                    foreach (string hex1 in ar)
                    {
                        if (chkerrorcon == "")
                        {
                            hex = hex1;
                        if (hex1 == "10D")
                        {
                            str = readdatameter32byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');

                            ReData[0] = 0x00;
                            ReData[1] = 0x00;
                            ReData[2] = Convert.ToByte(str1[14]);
                            ReData[3] = Convert.ToByte(str1[15]);
                            MakeDword[19] = CRC16.Read(ReData);
                            ReData[0] = 0x00;
                            ReData[1] = 0x00;
                            ReData[2] = Convert.ToByte(str1[0]);
                            ReData[3] = Convert.ToByte(str1[1]);
                            MakeDword[29] = CRC16.Read(ReData);
                            ReData[0] = 0x00;
                            ReData[1] = 0x00;
                            ReData[2] = Convert.ToByte(str1[2]);
                            ReData[3] = Convert.ToByte(str1[3]);
                            MakeDword[30] = CRC16.Read(ReData);

                        }
                        if (hex1 == "1A0")
                        {
                            str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');

                            //CRC16.cnt_kwh[Convert.ToByte(MeterAddr)]++;
                                
                            ReData[0] = Convert.ToByte(str1[0]);
                            ReData[1] = Convert.ToByte(str1[1]);
                            ReData[2] = Convert.ToByte(str1[2]);
                            ReData[3] = Convert.ToByte(str1[3]);
                            MakeDword[25] = CRC16.Read(ReData);

                            //if (CRC16.Read(ReData) != 0)
                            //{
                            //    CRC16.chk_kwh0[Convert.ToInt16(meterid)] = CRC16.Read(ReData); 
                            //}
                            //else
                            //{
                            //    MakeDword[25] = CRC16.chk_kwh0[Convert.ToInt16(meterid)];
                            //}
                                

                            //CRC16.chk_kwh[Convert.ToByte(MeterAddr), CRC16.cnt_kwh[Convert.ToByte(MeterAddr)]] = MakeDword[25]/30;
                            //if (CRC16.cnt_kwh[Convert.ToByte(MeterAddr)] == 3)
                            //{
                            //    if (CRC16.chk_kwh[Convert.ToByte(MeterAddr), 1] <( MakeDword[25] / 30))
                            //    {

                            //    }
                            //    CRC16.cnt_kwh[Convert.ToByte(MeterAddr)] = 0;
                            //}
                            //if (meterid == "0331")
                            //{
                            //    MakeDword[25] = CRC16.Read(ReData);
                            //}
                               
                        }
                        if (hex1 == "1A2")
                        {
                            str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1, System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');

                            ReData[0] = Convert.ToByte(str1[0]);
                            ReData[1] = Convert.ToByte(str1[1]);
                            ReData[2] = Convert.ToByte(str1[2]);
                            ReData[3] = Convert.ToByte(str1[3]);
                            MakeDword[28] = CRC16.Read(ReData);
                            //if (MeterAddr == "236")
                            //{
                            //    MakeDword[28] = CRC16.Read(ReData);
                            //}
                           
                        }

                        if (hex1 == "C8")
                        {
                            str = readdatameter32byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1.ToString(), System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');
                            //using (StreamWriter sw = File.AppendText(path))
                            //{
                            //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() +
                            //        " JanitzaUMG96S hex= " + hex1 + "meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> " +
                            //        "B1 = " + str1[3] + " B2 = " + str1[4] + " B3 = " + str1[5] + " B4 = " + str1[6] +
                            //        "  read6byte = " + str + "----> Read Data");
                            //}

                            for (int i = 0; i < cloop; i += bytes[c])
                            {
                                try
                                {

                                    ReData[0] = 0x00;
                                    ReData[1] = 0x00;
                                    //if (Convert.ToByte(str1[i + 0]) > 255)
                                    //{
                                    //    ReData[2] = 0;
                                    //}
                                    //else
                                    //{
                                    //    ReData[2] = Convert.ToByte(str1[i + 0]);
                                    //}
                                    //if (Convert.ToByte(str1[i + 0]) > 255)
                                    //{
                                    //    ReData[3] = 0;
                                    //}
                                    //else
                                    //{
                                    //    ReData[3] = Convert.ToByte(str1[i + 1]);
                                    //}
                                    ReData[2] = Convert.ToByte(str1[i + 0]);
                                    ReData[3] = Convert.ToByte(str1[i + 1]);

                                    MakeDword[c] = CRC16.Read(ReData);
                                    c++;
                                }
                                catch
                                {
                                    //using (StreamWriter sw = File.AppendText(path))
                                    //{
                                    //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() +
                                    //        " JanitzaUMG96S hex= " + hex1 + "meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> "  );
                                    //}
                                
                                }

                            }
                            //if (meterid == "0236")
                            //{
                            //    MakeDword[c] = CRC16.Read(ReData);
                            //}
                            //if (meterid == "0331")
                            //{
                            //    MakeDword[c] = CRC16.Read(ReData);
                            //}
                          
                         }
                           
                        }
                    }
                    if (chkerrorcon == "")
                    {
                        if (str != "")
                        {
                            str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse("258", System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');
                            c = 29;
                            for (int a = 0; a < 8; a += 2)
                            {

                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = Convert.ToByte(str1[a + 0]);
                                ReData[3] = Convert.ToByte(str1[a + 1]);
                                MakeDword[c] = CRC16.Read(ReData);
                                c++;
                            }
                        }
                    }
                    
                    MakeDword[34] = Convert.ToInt32(str1[1]);
                    if(MeterAddr == "3")
                        datameter = new double[35];
                    datameter = new double[35];
                    setdenominator();
                    for (int l = 0; l < 35; l++)
                    {
                        datameter[l] = MakeDword[l] / denominator[l]; // เอาค่าที่ set ไว้มาหาร
                    }
                    double CT, CT1;
                    if (meterid == "0236")
                    {
                        CT = datameter[29] / datameter[30];//หารค่า CT 
                    }
                    CT = datameter[29] / datameter[30];//หารค่า CT 
                    CT1 = Math.Round(1000 / CT,2);//ค่าสูงสุดของ CT / ด้วยค่า CT ที่หารได้
                    
                    if (!HasValue(CT))
                    {
                        CT1 = 1;
                    }
                    datameter[6] = Math.Round(datameter[6] / CT1 ,2);//Amp1
                    datameter[7] = Math.Round(datameter[7] / CT1 ,2);//Amp2
                    datameter[8] = Math.Round(datameter[8] / CT1 ,2);//Amp3
                    datameter[9] = Math.Round(datameter[9] / CT1 ,2);//kW1
                    datameter[10] = Math.Round(datameter[10] / CT1 ,2);//kW2
                    datameter[6] = Math.Round(datameter[11] / CT1 ,2);//kW3
                    if (datameter[19] > 32768)// เช็คค่า pf 
                        datameter[19] = (65536 - datameter[19]) * -1;//ถ้าค่า pf มากกว่า 32768 ให้ลบ
                    if (datameter[19] > 100) { datameter[19] = Math.Round(datameter[19] / 1000, 2); }
                    else if (datameter[19] < 0) { datameter[19] = Math.Round((datameter[19] / 100)*(-1), 2); }
                    else{ datameter[19] = Math.Round(datameter[19] / 100, 2);}//pf
                    datameter[21] = Math.Round(datameter[21] / CT1 ,2);//AmpN
                    datameter[22] = datameter[29]; //Math.Round(datameter[22] / CT1 ,2);//kW
                    if (datameter[23] > 32768)// เช็คค่า kvar
                        datameter[23] = (65536 - datameter[23]) * -1;//ถ้าค่า kvar มากกว่า 32768 ให้ลบ
                    datameter[23] = Math.Round(datameter[23] / CT1 ,2);//kVAr
                    datameter[23] = datameter[30]; ;//Math.Round(datameter[23] / CT1 ,2);//kVAr
                    datameter[24] = Math.Round(datameter[24] / CT1 ,2);//kVA
                    if ((datameter[25] / CT1) > 500000)
                    {
                        datameter[25] = 0;
                    }
                    else
                    {
                        datameter[25] = Math.Round(datameter[25] / CT1, 2);//kWh
                    }

                    if (((datameter[28] / CT1) > 500000) || ((datameter[28] / CT1) < 0))
                    {
                        datameter[28] = 0;
                    }
                    else
                    {
                        datameter[28] = Math.Round(datameter[28] / CT1, 2);//kVAhr
                    }
                    datameter[33] = (datameter[0] + datameter[1] + datameter[2]) / 3; //VoltAvg
                    datameter[34] = (datameter[6] + datameter[7] + datameter[8]) / 3; //AmpAvg
                    if (meterid == "0236" || meterid == "0332" || meterid == "0334" || meterid == "0335" || meterid == "0336") { 
                    using (StreamWriter sw = File.AppendText(path))
                    {
                        sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() + "JanitzaUMG96S meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> " + "CT " + datameter[29].ToString() + "/" + datameter[30].ToString() + " kwh = " + datameter[25].ToString() + " kvarh = " + datameter[28].ToString());
                    }
                    }
                    if (meterid == "8")
                    {
                        
                    }

                    UMG96S(datameter);
                    port.closeSP(ComPort);                                    
                }
                catch (Exception)
                {
                   port.closeSP(ComPort);
                  // MessageBox.Show("Close port JanitzaUMG96S" + "meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> "  ); 
                   using (StreamWriter sw = File.AppendText(path))
                   {
                       sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() + "Close port JanitzaUMG96S HEX=" + hex + " meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " meterName <" + meterName + "> ");
                   }
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
            Thread.Sleep(10);

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
                        bool connect = ConnDB.connect3();
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
                        bool connect = ConnDB.connect3();
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
                for (int c = 0; c < 8; c++)
                {
                    ReData[c] = Alldata[c + 3];
                    valuemeter += ReData[c].ToString() + ",";
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
            Thread.Sleep(10);

            byte[] Alldata = new byte[105];

            int offset = 0;
            int offset1 = 1;
            int xxxx;
            int remaining = Alldata.Length;
            //int bytes = ComPort.BytesToRead;  //find the size of the array needed
            //] Alldata = new byte[bytes];  //create the array
            if (meterid == "0236" || meterid == "0332" || meterid == "0334" || meterid == "0335" || meterid == "0336")
                {
                    //int bytes = ComPort.BytesToRead;  //find the size of the array needed
                    //byte[] buffer = new byte[bytes];  //create the array
                    //ComPort.Read(buffer, 0, bytes);  //read the message and save it into the buffer
                    //byte[] ReceivedMessage = new byte[bytes];  //create an array of the same size
                    //Array.Copy(buffer, ReceivedMessage, bytes);  //copy it across

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
                        bool connect = ConnDB.connect3();
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
                        bool connect = ConnDB.connect3();
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
                byte[] ReData = new byte[100];
                //valuemeter = Alldata[0].ToString() + "," + Alldata[1].ToString() + "," + Alldata[2].ToString() + "," + Alldata[3].ToString() + "," +
                //            Alldata[4].ToString() + "," + Alldata[5].ToString() + "," + Alldata[6].ToString() + "," + Alldata[7].ToString() + "," +
                //        Alldata[8].ToString() + "," + Alldata[9].ToString() + "," + Alldata[10].ToString() + "," + Alldata[11].ToString() + "," +
                //        Alldata[12].ToString() + "," + Alldata[13].ToString() + "," + Alldata[14].ToString() + "," + Alldata[15].ToString() + "," +
                //        Alldata[16].ToString();
                //using (StreamWriter sw = File.AppendText(path1))
                //{
                //    sw.WriteLine(DateTime.Now.ToShortDateString() + "->" + DateTime.Now.ToShortTimeString() +
                //        " JanitzaUMG96S  meter_id = " + meterid + " ADDRESS<" + MeterAddr + ">" + " <" + meterName + "> " +
                //        "B1 = " + str1[3] + " B2 = " + str1[4] + " B3 = " + str1[5] + " B4 = " + str1[6] +
                //        "  read32byte = " + valuemeter + "----> Read Data");
                //}
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
                
                if (Alldata[0] == (byte)addr && Alldata[1] == 0x03 && Alldata[2] == 0x64)
                {
                    for (int c = 0; c < 100; c++)
                    {
                        ReData[c] = Alldata[c + 3];
                        valuemeter += ReData[c].ToString() + ",";
                    }
                }
               
                else
                {
                    for (int c = 0; c < 100; c++)
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
        private void UMG96S(double [] data)
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
                dr["kW"] = String.Format("{0:0.00}",data[22]);
                dr["kW1"] = String.Format("{0:0.00}",data[9]);
                dr["kW2"] = String.Format("{0:0.00}",data[10]);
                dr["kW3"] = String.Format("{0:0.00}",data[11]);
                dr["kVAr"] = String.Format("{0:0.00}",data[23]);
                dr["kWh"] = String.Format("{0:0.00}",data[25]);
                dr["kVA"] = String.Format("{0:0.00}",data[24]);
                dr["kVArh"] = String.Format("{0:0.00}",data[28]);
                dr["VoltP1"] = String.Format("{0:0.00}",data[0]);
                dr["VoltP2"] = String.Format("{0:0.00}",data[1]);
                dr["VoltP3"] = String.Format("{0:0.00}",data[2]);
                dr["VoltAvg"] = String.Format("{0:0.00}",data[33]);
                dr["VoltL1"] = String.Format("{0:0.00}",data[3]);
                dr["VoltL2"] = String.Format("{0:0.00}",data[4]);
                dr["VoltL3"] = String.Format("{0:0.00}",data[5]);
                dr["Amp1"] = String.Format("{0:0.00}",data[6]);
                dr["Amp2"] = String.Format("{0:0.00}",data[7]);
                dr["Amp3"] = String.Format("{0:0.00}",data[8]);
                dr["AmpAvg"] = String.Format("{0:0.00}",data[34]);
                dr["AmpN"] = String.Format("{0:0.00}",data[21]);
                dr["pf"] = String.Format("{0:0.00}",data[19]);
                dr["Frequency"] = String.Format("{0:0.00}",data[18]);
                dr["THDV1"] = String.Format("{0:0.00}",data[12]);
                dr["THDV2"] = String.Format("{0:0.00}",data[13]);
                dr["THDV3"] = String.Format("{0:0.00}",data[14]);
                dr["THDA1"] = String.Format("{0:0.00}",data[15]);
                dr["THDA2"] = String.Format("{0:0.00}",data[16]);
                dr["THDA3"] = String.Format("{0:0.00}",data[17]);
                tb.Rows.Add(dr);
               
                    
                    SaveRealTime(dr);
                    //Save_1min(dr);

            }
            catch (Exception ex) { MessageBox.Show("ReadMySQL JanitzaUMG96S" + ex.Message); }
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
            bool chkdemand = chkrecord("eg_tt_data", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                }
                else
                {
                    ConnDB.sql = "update eg_tt_data " +
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
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string MeterData = String.Format("{0:0.00}",dr["kW"].ToString()) + "," + String.Format("{0:0.00}",dr["kW1"].ToString()) + "," + String.Format("{0:0.00}",dr["kW2"].ToString()) + "," + String.Format("{0:0.00}",dr["kW3"].ToString()) + "," + String.Format("{0:0.00}",dr["kVAr"].ToString()) + "," + String.Format("{0:0.00}",dr["kWh"].ToString()) + "," + String.Format("{0:0.00}",dr["kVA"].ToString()) + "," + String.Format("{0:0.00}",dr["kVArh"].ToString()) + "," +
              String.Format("{0:0.00}",dr["VoltP1"].ToString()) + "," + String.Format("{0:0.00}",dr["VoltP2"].ToString()) + "," + String.Format("{0:0.00}",dr["VoltP3"].ToString()) + "," + String.Format("{0:0.00}",dr["VoltAvg"].ToString()) + "," + String.Format("{0:0.00}",dr["VoltL1"].ToString()) + "," + String.Format("{0:0.00}",dr["VoltL2"].ToString()) + "," + String.Format("{0:0.00}",dr["VoltL3"].ToString()) + "," +
              String.Format("{0:0.00}",dr["Amp1"].ToString()) + "," + String.Format("{0:0.00}",dr["Amp2"].ToString()) + "," + String.Format("{0:0.00}",dr["Amp3"].ToString()) + "," + String.Format("{0:0.00}",dr["AmpAvg"].ToString()) + "," + String.Format("{0:0.00}",dr["AmpN"].ToString()) + "," + String.Format("{0:0.00}",dr["pf"].ToString()) + "," + String.Format("{0:0.00}",dr["frequency"].ToString()) + "," + String.Format("{0:0.00}",dr["THDV1"].ToString()) + "," + String.Format("{0:0.00}",dr["THDV2"].ToString()) + "," +
              String.Format("{0:0.00}",dr["THDV3"].ToString()) + "," + String.Format("{0:0.00}",dr["THDA1"].ToString()) + "," + String.Format("{0:0.00}",dr["THDA2"].ToString()) + "," + String.Format("{0:0.00}",dr["THDA3"].ToString());
            bool chkdemand = chkrecord("dbrtftyrv2.eg_tt_data", "where meter_id ='" + meterid + "'");

            //byte[] bytes = Encoding.Default.GetBytes(MeterData);
            //MeterData = Encoding.UTF8.GetString(bytes);
            bool connect = ConnDB.connect3();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                }
                else
                {
                    ConnDB.sql = "update eg_tt_data " +
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
                    MessageBox.Show(string.Format("JanitzaUMG96S ทดสอบค่า Meter {0} CT:{1} tt_data" + ex.Message, meterid , chkdemand));
                    ConnDB.reader.Close();
                }
                
            }
            ConnDB.clossDB();
        }

        /*private void Save_1min(DataRow dr)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, now.Minute, 0);
            string Time = date.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = date.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_minutes", "where hour_id='" + Time + "' and meter_id ='" + meterid + "'");

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
                    ConnDB.sql = "insert into eg_tr_minutes " +
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
                    ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + MeterData + "' where hour_id=@hour_id and meter_id=@meter_id ";
                }
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
        }*/

       /* private void Save_15min(DataTable tb)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
            DateTime minute = date.AddMinutes((now.Minute / 15) * 15);
            string Time = minute.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = minute.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            string time_begin = minute.AddMinutes(-15).ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            string time_final = minute.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_demand", "where hour_id='" + Time + "' and meter_id ='" + meterid + "'");
            DataRow[] begin, final;
            final = tb.Select("Begin = " + time_final);
            double unit = 0;
            foreach (DataRow dr in final)
            {
                begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                double kwh_begin = begin.Length != 0 ? 0 : Convert.ToDouble(dr["kWh"]);//Convert.ToDouble(begin[0]["kWh"])
                unit = Convert.ToDouble(dr["kWh"]) - kwh_begin;

                string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                string[] eg = new string[4];
                eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                string MeterData = dr["kW"] + "," + dr["kW1"] + "," + dr["kW2"] + "," + dr["kW3"] + "," + dr["kVAr"] + "," + kwh_begin.ToString("0.00") + "," + dr["kWh"] + "," + unit + "," +
                  dr["kVA"] + "," + dr["kVArh"] + "," + dr["VoltP1"] + "," + dr["VoltP2"] + "," + dr["VoltP3"] + "," + dr["VoltL1"] + "," + dr["VoltL2"] + "," + dr["VoltL3"] + "," +
                  dr["Amp1"] + "," + dr["Amp2"] + "," + dr["Amp3"] + "," + dr["AmpN"] + "," + dr["pf"] + "," + dr["frequency"] + "," + dr["THDV1"] + "," + dr["THDV2"] + "," +
                  dr["THDV3"] + "," + dr["THDA1"] + "," + dr["THDA2"] + "," + dr["THDA3"];

                bool connect = ConnDB.connect();
                if (connect)
                {
                    if (!chkdemand)
                    {
                        ConnDB.sql = "insert into eg_tr_demand " +
                        "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                    }
                    else
                    {
                        ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + MeterData + "' where hour_id=@hour_id and meter_id=@meter_id";
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
        }*/

        /*private void Save_1hour(DataTable tb)
        {
            DateTime now = DateTime.Now;
            DateTime date = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
            string Time = date.ToString("yyyyMMdd", new System.Globalization.CultureInfo("en-US"));
            string HH = date.ToString("HH", new System.Globalization.CultureInfo("en-US"));
            string time_begin = date.AddHours(-1).ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            string time_final = date.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_hour", "where day_id='" + Time + "' and meter_id ='" + meterid + "'");
            DataRow[] begin, final;
            final = tb.Select("Begin = " + time_final);
            double unit = 0;
            foreach (DataRow dr in final)
            {
                begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                double kwh_begin = begin.Length != 0 ? 0 : Convert.ToDouble(dr["kWh"]);//Convert.ToDouble(begin[0]["kWh"])
                unit = Convert.ToDouble(dr["kWh"]) - kwh_begin;

                string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                string[] eg = new string[24];
                eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                string MeterData = dr["kW"] + "," + dr["kW1"] + "," + dr["kW2"] + "," + dr["kW3"] + "," + dr["kVAr"] + "," + kwh_begin.ToString("0.00") + "," + dr["kWh"] + "," + unit + "," +
                  dr["kVA"] + "," + dr["kVArh"] + "," + dr["VoltP1"] + "," + dr["VoltP2"] + "," + dr["VoltP3"] + "," + dr["VoltL1"] + "," + dr["VoltL2"] + "," + dr["VoltL3"] + "," +
                  dr["Amp1"] + "," + dr["Amp2"] + "," + dr["Amp3"] + "," + dr["AmpN"] + "," + dr["pf"] + "," + dr["frequency"] + "," + dr["THDV1"] + "," + dr["THDV2"] + "," +
                  dr["THDV3"] + "," + dr["THDA1"] + "," + dr["THDA2"] + "," + dr["THDA3"];
                bool connect = ConnDB.connect();
                if (connect)
                {
                    if (!chkdemand)
                    {
                        ConnDB.sql = "insert into eg_tr_hour " +
                        "values (@day_id,@meter_id,@energy_type,'" +
                        eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                        eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                        eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                    }
                    else
                    {
                        ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + MeterData + "' where day_id=@day_id and meter_id=@meter_id";
                    }
                    ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                    ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                    ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                    ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                    ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                    ConnDB.reader.Close();
                }
                ConnDB.clossDB();
            }
        }*/

        private void XMLUMG966S()
        {
            if (File.Exists(pathmeter + meterName + ".xml") == false)
            {
                XmlTextWriter writer = new XmlTextWriter(pathmeter + meterName + ".xml", System.Text.Encoding.UTF8);
                writer.WriteStartDocument(true);
                writer.Formatting = Formatting.Indented;
                writer.Indentation = 2;
                writer.WriteStartElement("ReadMeter");
                UMG96S(writer);
                writer.WriteEndElement();
                writer.WriteEndDocument();
                writer.Close();
            }
            else { UpdateUMG96S(); }
        }

        private void UMG96S(XmlTextWriter writer)
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
            catch (Exception e) { MessageBox.Show("ReadXML !!" + e.Message); }
        }

        private void UpdateUMG96S()
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

    }
}
