package ru.gigabyteartur.weights_of_war.neuro_net;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Нейронная сеть.
public class NeuroNet
{
    public final static int NEURO_NET_FORMAT_VERSION = 7;       // Версия формата сохранения нейронной сети. Поднимать при каждом изменении XML-формата.

    private static NeuroNet logTarget = null;                   // Сеть, состояние которой нужно залогировать при следующем CalcSignals.
    private static String logFileName = null;                   // Имя файла для лога состояния сети.
    private static String logUnitName = null;                   // Имя юнита, которому принадлежит сеть.
    private static ArrayList<String> logInputNames = null;      // Имена сенсоров (входные нейроны).
    private static ArrayList<String> logOutputNames = null;     // Имена команд (выходные нейроны).

    private ArrayList<Layer> Layers = new ArrayList<>();

    // Добавляет в модель слой layer_in.
    public void AddLayer(Layer layer_in)
    {
        this.Layers.add(layer_in);
        layer_in.SetParentNeuroNet(this);
    }

    // Генерирует слой с размером count_in и добавляет его в модель.
    public void GenerateAddLayer(int count_in, boolean is_input_in, boolean is_output_in)
    {
        Layer new_layer = new Layer();
        new_layer.GenerateLayer(count_in, is_input_in, is_output_in);
        this.AddLayer(new_layer);
    }

    // Выводит модель на экран.
    public void Show()
    {
        for (Layer curr_layer:this.Layers)
        {
            curr_layer.ShowLayer();
        }
    }

    // Получает следующий за layer_in слой в модели.
    private Layer NextLayer(Layer layer_in)
    {
        Layer rez = new Layer();
        boolean found = false;
        for (Layer curr_layer:this.Layers)
        {
            if (found)
            {
                rez = curr_layer;
                break;
            }
            if (curr_layer == layer_in)
            {
                found = true;
            }
        }
        return rez;
    }

    // Компилирует модель.
    public void Compile()
    {
        Layer next_layer;
        ArrayList <Neuron> target_neurons;
        ArrayList <Neuron> source_neurons = new ArrayList<>();
        for (Layer curr_layer:this.Layers)
        {
            next_layer = this.NextLayer(curr_layer);
            if (next_layer != null)
            {
                target_neurons = next_layer.GetNeurons();
                source_neurons = curr_layer.GetNeurons();
                for (Neuron curr_target_neurons: target_neurons)
                {
                    for (Neuron curr_source_neurons:source_neurons)
                        curr_target_neurons.GenerateAddAxon(curr_source_neurons, 0);
                }
            }
        }
    }

//    // Обнуляет сигналы во всех слоях текущей сети.
//    public void EmptySignals()
//    {
//        for (Layer curr_layer:Layers)
//        {
//            curr_layer:EmptySignals();
//        }
//    }

    // Копирует модель из нейросети neuro_net_in в текущую (глубоко).
    public void CopyModel(NeuroNet neuro_net_in)
    {
        this.Layers.clear();

        HashMap<Neuron, Neuron> neuronMap = new HashMap<Neuron, Neuron>();

        // Копируем слои и нейроны.
        for (Layer layer : neuro_net_in.GetLayers())
        {
            Layer newLayer;
            if (layer instanceof DenseLayer)
            {
                newLayer = new DenseLayer();
            }
            else
            {
                newLayer = new Layer();
            }
            newLayer.GenerateLayer(layer.GetSize(), layer.GetIsInput(), layer.GetIsOutput());
            this.AddLayer(newLayer);

            ArrayList<Neuron> oldNeurons = layer.GetNeurons();
            ArrayList<Neuron> newNeurons = newLayer.GetNeurons();
            for (int i = 0; i < oldNeurons.size(); i++)
            {
                Neuron oldNeuron = oldNeurons.get(i);
                Neuron newNeuron = newNeurons.get(i);
                newNeuron.SetSignal(oldNeuron.GetSignal());
                newNeuron.SetBias(oldNeuron.GetBias());
                neuronMap.put(oldNeuron, newNeuron);
            }
        }

        // Копируем связи (аксоны).
        for (Layer layer : neuro_net_in.GetLayers())
        {
            for (Neuron oldNeuron : layer.GetNeurons())
            {
                Neuron newNeuron = neuronMap.get(oldNeuron);
                for (Axon oldAxon : oldNeuron.GetAxons())
                {
                    Neuron newSource = neuronMap.get(oldAxon.GetSource());
                    if (newSource != null)
                    {
                        newNeuron.GenerateAddAxon(newSource, oldAxon.GetWeight());
                    }
                }
            }
        }
    }

    // Возвращает глубокую копию текущей нейросети.
    public NeuroNet Copy()
    {
        NeuroNet copy = new NeuroNet();
        copy.CopyModel(this);
        return copy;
    }

    // Устанавливает во входном нейроне neuron_id_in значение сигнала signal_in.
    public void SetInputSignal(int neuron_id_in, double signal_in)
    {
        Layer input_layer;
        if (this.Layers.size() != 0)
        {
            input_layer = this.Layers.get(0);
            input_layer.SetSignalToNeuron(neuron_id_in, signal_in);
        }
        else
        {
            System.out.println("В нейронной сети недосточно слоёв для установки начального сигнала");
        }
    }

    // Вычисляет сигналы в текущей нейронной сети по слоям.
    public void CalcSignals()
    {
        ArrayList<Layer> layers = this.GetLayers();

        for (int layerIdx = 1; layerIdx < layers.size(); layerIdx++)
        {
            Layer currentLayer = layers.get(layerIdx);
            ArrayList<Neuron> neurons = currentLayer.GetNeurons();

            for (Neuron neuron : neurons)
            {
                // Сумма начинается со смещения (bias).
                double sum = neuron.GetBias();
                ArrayList<Axon> axons = neuron.GetAxons();

                // Суммируем входы: сигнал_источника * вес
                for (Axon axon : axons)
                {
                    Neuron source = axon.GetSource();
                    double weight = axon.GetWeight();
                    sum += source.GetSignal() * weight;
                }

                // Применяем функцию активации tanh
                double activated = Tanh(sum);
                neuron.SetSignal(activated);
            }
        }

        // Логирование состояния сети (если запланировано).
        if (this == logTarget && logFileName != null)
        {
            SaveToFile(logFileName, logUnitName, logInputNames, logOutputNames);
            logTarget = null;
            logFileName = null;
            logUnitName = null;
            logInputNames = null;
            logOutputNames = null;
        }
    }

    // Планирует логирование состояния сети target при её следующем расчёте сигналов.
    public static void ScheduleLogging(NeuroNet target, String fileName, String unitName, ArrayList<String> inputNames, ArrayList<String> outputNames)
    {
        logTarget = target;
        logFileName = fileName;
        logUnitName = unitName;
        logInputNames = inputNames;
        logOutputNames = outputNames;
    }

    // Гиперболический тангенс: преобразует сигнал в диапазон [-1, 1].
    private double Tanh(double signal_in)
    {
        return Math.tanh(signal_in);
    }

    // Возвращает сигнал последнего нейрона.
    public double GetOutputSignal()
    {
        double rez;
        rez = 0;
        int layers_size;
        Neuron first_neuron;
        ArrayList<Neuron> last_last_neurons = new ArrayList<>();
        Layer last_layer;
        layers_size = this.Layers.size();
        last_layer = this.Layers.get(layers_size -1);
        if (last_layer.GetSize() > 0)
        {
            last_last_neurons = last_layer.GetNeurons();
            first_neuron = last_last_neurons.get(0);
            rez = first_neuron.GetSignal();
        }
        else
        {
            rez = 0;
        }
        return rez;
    }

    // Устанавливает во все веса случайные значения.
    public void RandomWeights()
    {
        double new_weight;
        final Random random = new Random();
        for (Layer curr_layer:this.Layers)
        {
            for (Neuron curr_neuron: curr_layer.GetNeurons())
            {
                for (Axon curr_axon: curr_neuron.GetAxons())
                {
                    new_weight = (random.nextDouble()*2) - 1;
                    curr_axon.SetWeight(new_weight);
                }
                // Начальное смещение (bias) в диапазоне -1..1.
                curr_neuron.SetBias((random.nextDouble() * 2) - 1);
            }
        }
    }

    // С вероятностью probablity_in устанавливает всем аксонам текущей нейронной сети
    // случайные веса.
    public void Mutate(double probablity_in)
    {
        for (Layer curr_layer:this.Layers)
        {
            curr_layer.Mutate(probablity_in);
        }
    }

    // Сохраняет текущую нейросеть в XML-файл с именем file_name_in (без подписей).
    public void SaveToFile(String file_name_in)
    {
        SaveToFile(file_name_in, null, null, null);
    }

    // Сохраняет текущую нейросеть в XML-файл с подписями: имя юнита,
    // имена входных сенсоров и выходных команд.
    public void SaveToFile(String file_name_in, String unitName, ArrayList<String> inputNames, ArrayList<String> outputNames)
    {
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // Корневой элемент — нейросеть.
            Element root_element = doc.createElement("neuro_net");
            doc.appendChild(root_element);

            // Версия формата сохранения.
            Element format_version_element = doc.createElement("format_version");
            root_element.appendChild(format_version_element);
            format_version_element.appendChild(doc.createTextNode(String.valueOf(NEURO_NET_FORMAT_VERSION)));

            // Имя юнита, которому принадлежит сеть.
            if (unitName != null)
            {
                Element unit_name_element = doc.createElement("unit_name");
                root_element.appendChild(unit_name_element);
                unit_name_element.appendChild(doc.createTextNode(unitName));
            }

            // Слои.
            Element layers_element = doc.createElement("layers");
            root_element.appendChild(layers_element);

            for (Layer curr_layer : this.Layers)
            {
                // Слой.
                Element layer_element = doc.createElement("layer");
                layers_element.appendChild(layer_element);

                // Идентификатор слоя.
                Element layer_id_element = doc.createElement("id");
                layer_element.appendChild(layer_id_element);
                layer_id_element.appendChild(doc.createTextNode(curr_layer.GetId()));

                // Признак входного слоя.
                Element layer_is_input_element = doc.createElement("is_input");
                layer_element.appendChild(layer_is_input_element);
                layer_is_input_element.appendChild(doc.createTextNode(curr_layer.GetIsInput() ? "Yes" : "No"));

                // Признак выходного слоя.
                Element layer_is_output_element = doc.createElement("is_output");
                layer_element.appendChild(layer_is_output_element);
                layer_is_output_element.appendChild(doc.createTextNode(curr_layer.GetIsOutput() ? "Yes" : "No"));

                // Нейроны.
                Element neurons_element = doc.createElement("neurons");
                layer_element.appendChild(neurons_element);

                ArrayList<Neuron> neurons = curr_layer.GetNeurons();
                for (int n = 0; n < neurons.size(); n++)
                {
                    Neuron curr_neuron = neurons.get(n);

                    // Нейрон.
                    Element neuron_element = doc.createElement("neuron");
                    neurons_element.appendChild(neuron_element);

                    // Идентификатор нейрона.
                    Element neuron_id_element = doc.createElement("id");
                    neuron_element.appendChild(neuron_id_element);
                    neuron_id_element.appendChild(doc.createTextNode(curr_neuron.GetId()));

                    // Подпись нейрона: сенсор для входного слоя, команда для выходного.
                    String label = null;
                    if (curr_layer.GetIsInput() && inputNames != null && n < inputNames.size())
                    {
                        label = inputNames.get(n);
                    }
                    else if (curr_layer.GetIsOutput() && outputNames != null && n < outputNames.size())
                    {
                        label = outputNames.get(n);
                    }
                    if (label != null)
                    {
                        Element neuron_label_element = doc.createElement("label");
                        neuron_element.appendChild(neuron_label_element);
                        neuron_label_element.appendChild(doc.createTextNode(label));
                    }

                    // Сигнал нейрона.
                    Element neuron_signal_element = doc.createElement("signal");
                    neuron_element.appendChild(neuron_signal_element);
                    neuron_signal_element.appendChild(doc.createTextNode(String.valueOf(curr_neuron.GetSignal())));

                    // Смещение (bias) нейрона.
                    Element neuron_bias_element = doc.createElement("bias");
                    neuron_element.appendChild(neuron_bias_element);
                    neuron_bias_element.appendChild(doc.createTextNode(String.valueOf(curr_neuron.GetBias())));

                    // Аксоны.
                    Element axons_element = doc.createElement("axons");
                    neuron_element.appendChild(axons_element);

                    for (Axon curr_axon : curr_neuron.GetAxons())
                    {
                        // Аксон.
                        Element axon_element = doc.createElement("axon");
                        axons_element.appendChild(axon_element);

                        // Идентификатор входного (источника) нейрона.
                        Element source_neuron_id_element = doc.createElement("neuron_id");
                        axon_element.appendChild(source_neuron_id_element);

                        // Вес связи.
                        Element weight_element = doc.createElement("weight");
                        axon_element.appendChild(weight_element);

                        Neuron source_neuron = curr_axon.GetSource();
                        if (source_neuron != null)
                        {
                            source_neuron_id_element.appendChild(doc.createTextNode(source_neuron.GetId()));
                            weight_element.appendChild(doc.createTextNode(String.valueOf(curr_axon.GetWeight())));
                        }
                        else
                        {
                            source_neuron_id_element.appendChild(doc.createTextNode(""));
                            weight_element.appendChild(doc.createTextNode("0"));
                        }
                    }
                }
            }

            // Запись в XML-файл.
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(file_name_in));
            transformer.transform(source, result);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Загружает нейросеть из XML-файла с именем file_name_in.
    // Возвращает true при успешной загрузке; false — если версия формата в файле ниже текущей (сеть оставлена пустой).
    public boolean LoadFromFile(String file_name_in)
    {
        try
        {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(file_name_in);
            Node root_item = document.getDocumentElement();

            this.Layers.clear();

            NodeList root_children = root_item.getChildNodes();

            // Определяем версию формата в файле (0, если элемент отсутствует или некорректен).
            int file_format_version = 0;
            for (int i = 0; i < root_children.getLength(); i++)
            {
                Node root_child = root_children.item(i);
                if (root_child.getNodeType() == Node.ELEMENT_NODE && root_child.getNodeName().equals("format_version"))
                {
                    try
                    {
                        file_format_version = Integer.parseInt(root_child.getTextContent().trim());
                    }
                    catch (NumberFormatException e)
                    {
                        file_format_version = 0;
                    }
                    break;
                }
            }

            // Если версия файла ниже текущей — не загружаем данные, оставляем новую (пустую) сеть.
            if (file_format_version < NEURO_NET_FORMAT_VERSION)
            {
                return false;
            }

            Layer last_layer = null;

            for (int i = 0; i < root_children.getLength(); i++)
            {
                Node root_child = root_children.item(i);
                if (root_child.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }

                String root_child_name = root_child.getNodeName();
                if (root_child_name.equals("layers"))
                {
                    NodeList layer_list = root_child.getChildNodes();
                    for (int c_layer = 0; c_layer < layer_list.getLength(); c_layer++)
                    {
                        Node layer_item = layer_list.item(c_layer);
                        if (layer_item.getNodeType() != Node.ELEMENT_NODE)
                        {
                            continue;
                        }

                        Layer new_layer = new Layer();
                        String layer_id = "";
                        boolean is_input = false;
                        boolean is_output = false;

                        NodeList layer_children = layer_item.getChildNodes();
                        for (int c_attr = 0; c_attr < layer_children.getLength(); c_attr++)
                        {
                            Node layer_child = layer_children.item(c_attr);
                            if (layer_child.getNodeType() != Node.ELEMENT_NODE)
                            {
                                continue;
                            }

                            String layer_child_name = layer_child.getNodeName();
                            if (layer_child_name.equals("id"))
                            {
                                layer_id = layer_child.getTextContent();
                            }
                            else if (layer_child_name.equals("is_input"))
                            {
                                is_input = layer_child.getTextContent().equals("Yes");
                            }
                            else if (layer_child_name.equals("is_output"))
                            {
                                is_output = layer_child.getTextContent().equals("Yes");
                            }
                            else if (layer_child_name.equals("neurons"))
                            {
                                NodeList neuron_list = layer_child.getChildNodes();
                                for (int c_neuron = 0; c_neuron < neuron_list.getLength(); c_neuron++)
                                {
                                    Node neuron_item = neuron_list.item(c_neuron);
                                    if (neuron_item.getNodeType() != Node.ELEMENT_NODE)
                                    {
                                        continue;
                                    }

                                    Neuron new_neuron = new Neuron();
                                    String neuron_id = "";

                                    NodeList neuron_children = neuron_item.getChildNodes();
                                    for (int c_nattr = 0; c_nattr < neuron_children.getLength(); c_nattr++)
                                    {
                                        Node neuron_child = neuron_children.item(c_nattr);
                                        if (neuron_child.getNodeType() != Node.ELEMENT_NODE)
                                        {
                                            continue;
                                        }

                                        String neuron_child_name = neuron_child.getNodeName();
                                        if (neuron_child_name.equals("id"))
                                        {
                                            neuron_id = neuron_child.getTextContent();
                                        }
                                        else if (neuron_child_name.equals("bias"))
                                        {
                                            new_neuron.SetBias(Double.valueOf(neuron_child.getTextContent()));
                                        }
                                        else if (neuron_child_name.equals("axons"))
                                        {
                                            NodeList axon_list = neuron_child.getChildNodes();
                                            for (int c_axon = 0; c_axon < axon_list.getLength(); c_axon++)
                                            {
                                                Node axon_item = axon_list.item(c_axon);
                                                if (axon_item.getNodeType() != Node.ELEMENT_NODE)
                                                {
                                                    continue;
                                                }

                                                Axon new_axon = new Axon();
                                                double new_weight = 0;
                                                String source_neuron_id = "";

                                                NodeList axon_children = axon_item.getChildNodes();
                                                for (int c_aattr = 0; c_aattr < axon_children.getLength(); c_aattr++)
                                                {
                                                    Node axon_child = axon_children.item(c_aattr);
                                                    if (axon_child.getNodeType() != Node.ELEMENT_NODE)
                                                    {
                                                        continue;
                                                    }

                                                    String axon_child_name = axon_child.getNodeName();
                                                    if (axon_child_name.equals("neuron_id"))
                                                    {
                                                        source_neuron_id = axon_child.getTextContent();
                                                    }
                                                    else if (axon_child_name.equals("weight"))
                                                    {
                                                        new_weight = Double.valueOf(axon_child.getTextContent());
                                                    }
                                                }

                                                if (!source_neuron_id.equals("") && last_layer != null)
                                                {
                                                    Neuron source_neuron = last_layer.FindNeuronById(source_neuron_id);
                                                    new_axon.Set(source_neuron, new_weight);
                                                }
                                                new_neuron.AddAxon(new_axon);
                                            }
                                        }
                                    }

                                    new_neuron.SetId(neuron_id);
                                    new_layer.AddNeuron(new_neuron);
                                }
                            }
                        }

                        new_layer.SetOptions(layer_id, is_input, is_output);
                        this.AddLayer(new_layer);
                        last_layer = new_layer;
                    }
                }
            }

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    // Возвращает слои текущей сети.
    public ArrayList<Layer> GetLayers()
    {
        return Layers;
    }
}
